package dev.classityreal.pext.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.classityreal.pext.native.ExtractionEngine
import dev.classityreal.pext.native.NativeEntry
import dev.classityreal.pext.native.NativeToolException
import dev.classityreal.pext.native.PacExtractorRunner
import dev.classityreal.pext.native.SafBridge
import dev.classityreal.pext.native.UnpacRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface Screen {
    data object Home : Screen
    data object Loading : Screen
    data class SelectPartitions(val entries: List<NativeEntry>) : Screen
    data class Extracting(val engine: ExtractionEngine, val lastLine: String) : Screen
    data class Done(val outputDir: Uri) : Screen

    // Batch ("Select from list") flow
    data class SelectMultipleFiles(val files: List<PickedPac>) : Screen
    data class BatchExtracting(val currentIndex: Int, val total: Int, val currentName: String, val lastLine: String) : Screen
    data class BatchDone(val folderNames: List<String>) : Screen

    data class Error(val message: String, val detail: String = "") : Screen
}

/** One .pac the user picked for batch extraction, with its display name and byte size. */
data class PickedPac(val uri: Uri, val name: String, val size: Long)

class PacViewModel(app: Application) : AndroidViewModel(app) {

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _selectedNames = MutableStateFlow<Set<String>>(emptySet())
    val selectedNames: StateFlow<Set<String>> = _selectedNames.asStateFlow()

    private val _engine = MutableStateFlow(ExtractionEngine.UNPAC)
    val engine: StateFlow<ExtractionEngine> = _engine.asStateFlow()

    private val workDir get() = File(getApplication<Application>().cacheDir, "pac_work")

    fun setEngine(e: ExtractionEngine) {
        _engine.value = e
    }

    fun onPacFilePicked(uri: Uri) {
        _screen.value = Screen.Loading
        viewModelScope.launch {
            val result = runCatching { stageAndList(uri) }
            result.onSuccess { entries ->
                _selectedNames.value = entries.map { it.name }.toSet()
                _screen.value = Screen.SelectPartitions(entries)
            }.onFailure { e ->
                _screen.value = errorFrom(e)
            }
        }
    }

    private suspend fun stageAndList(uri: Uri): List<NativeEntry> = withContext(Dispatchers.IO) {
        val app = getApplication<Application>()
        workDir.deleteRecursively()
        val localPac = SafBridge.copyUriToCache(app, uri, workDir)
        UnpacRunner(app).list(localPac.absolutePath)
    }

    fun toggleEntry(name: String) {
        _selectedNames.update { current -> if (name in current) current - name else current + name }
    }

    fun selectAll(entries: List<NativeEntry>) {
        _selectedNames.value = entries.map { it.name }.toSet()
    }

    fun selectNone() {
        _selectedNames.value = emptySet()
    }

    fun startExtraction(outputTreeUri: Uri) {
        val app = getApplication<Application>()
        val localPac = File(workDir, "input.pac")
        val outDir = File(workDir, "out")
        val names = _selectedNames.value.toList()
        val chosenEngine = _engine.value

        if (chosenEngine == ExtractionEngine.UNPAC && names.isEmpty()) {
            _screen.value = Screen.Error("No partitions selected")
            return
        }

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    outDir.deleteRecursively()
                    when (chosenEngine) {
                        ExtractionEngine.UNPAC -> {
                            UnpacRunner(app).extract(localPac.absolutePath, outDir, names) { line ->
                                _screen.value = Screen.Extracting(chosenEngine, line)
                            }
                        }
                        ExtractionEngine.PAC_EXTRACTOR -> {
                            PacExtractorRunner(app).extractAll(localPac.absolutePath, outDir) { line ->
                                _screen.value = Screen.Extracting(chosenEngine, line)
                            }
                        }
                    }
                    SafBridge.copyDirToTree(app, outDir, outputTreeUri)
                    // Fully extracted and copied out to the user's chosen folder —
                    // nothing left in app cache is needed anymore, so clear it now
                    // instead of leaving it sitting there until the user taps Done.
                    workDir.deleteRecursively()
                    outputTreeUri
                }
            }
            result.onSuccess { uri -> _screen.value = Screen.Done(uri) }
                .onFailure { e -> _screen.value = errorFrom(e) }
        }
    }

    /** Step 1 of the batch flow: user picked several .pac files — look up their name/size before showing the summary. */
    fun onMultipleFilesPicked(uris: List<Uri>) {
        _screen.value = Screen.Loading
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val app = getApplication<Application>()
                    uris.map { uri ->
                        val name = SafBridge.queryDisplayName(app, uri) ?: "firmware.pac"
                        val size = SafBridge.querySize(app, uri) ?: 0L
                        PickedPac(uri, name, size)
                    }
                }
            }
            result.onSuccess { files ->
                _screen.value = if (files.isEmpty()) {
                    Screen.Error("No files were selected")
                } else {
                    Screen.SelectMultipleFiles(files)
                }
            }.onFailure { e -> _screen.value = errorFrom(e) }
        }
    }

    /** Free space on internal storage, for the "how much space is available" line on the selection screen. */
    fun freeSpaceBytes(): Long = SafBridge.internalStorageFreeBytes()

    /**
     * Step 2 of the batch flow, kicked off after the user grants access to a folder via
     * ACTION_OPEN_DOCUMENT_TREE. Creates (or reuses) a "PExt" folder inside that grant, then
     * one numbered subfolder per firmware ("Folder 1", "Folder 2", …), extracting everything
     * from each .pac into its own subfolder using pacextractor's extract-all mode — batch mode
     * doesn't offer per-partition selection, it always extracts the whole firmware.
     */
    fun startBatchExtraction(files: List<PickedPac>, outputTreeUri: Uri) {
        val app = getApplication<Application>()

        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val pextRoot = SafBridge.ensurePExtFolder(app, outputTreeUri)
                    val createdFolderNames = mutableListOf<String>()

                    files.forEachIndexed { index, pac ->
                        val position = index + 1
                        _screen.value = Screen.BatchExtracting(position, files.size, pac.name, "Preparing…")

                        val batchWorkDir = File(workDir, "batch_$index")
                        batchWorkDir.deleteRecursively()
                        val localPac = SafBridge.copyUriToCache(app, pac.uri, batchWorkDir)
                        val outDir = File(batchWorkDir, "out")
                        outDir.deleteRecursively()

                        PacExtractorRunner(app).extractAll(localPac.absolutePath, outDir) { line ->
                            _screen.value = Screen.BatchExtracting(position, files.size, pac.name, line)
                        }

                        val subfolder = SafBridge.createNumberedSubfolder(pextRoot, position)
                        SafBridge.copyDirToDocumentFile(app, outDir, subfolder)
                        createdFolderNames += subfolder.name ?: "Folder $position"
                        batchWorkDir.deleteRecursively()
                    }

                    // All firmwares extracted and copied out — nothing left in app
                    // cache is needed, clear it now rather than waiting for reset().
                    workDir.deleteRecursively()
                    createdFolderNames
                }
            }
            result.onSuccess { names -> _screen.value = Screen.BatchDone(names) }
                .onFailure { e -> _screen.value = errorFrom(e) }
        }
    }

    private fun errorFrom(e: Throwable): Screen.Error = when (e) {
        is NativeToolException -> Screen.Error(e.message ?: "Native tool failed", e.stdout)
        else -> Screen.Error(e.message ?: "Unexpected error: ${e::class.simpleName}")
    }

    fun reset() {
        _selectedNames.value = emptySet()
        _screen.value = Screen.Home
        workDir.deleteRecursively()
    }
}
