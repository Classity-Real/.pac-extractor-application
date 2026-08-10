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
    data object PickFile : Screen
    data object Loading : Screen
    data class SelectPartitions(val entries: List<NativeEntry>) : Screen
    data class Extracting(val engine: ExtractionEngine, val lastLine: String) : Screen
    data class Done(val outputDir: Uri) : Screen
    data class Error(val message: String, val detail: String = "") : Screen
}

class PacViewModel(app: Application) : AndroidViewModel(app) {

    private val _screen = MutableStateFlow<Screen>(Screen.PickFile)
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
                    outputTreeUri
                }
            }
            result.onSuccess { uri -> _screen.value = Screen.Done(uri) }
                .onFailure { e -> _screen.value = errorFrom(e) }
        }
    }

    private fun errorFrom(e: Throwable): Screen.Error = when (e) {
        is NativeToolException -> Screen.Error(e.message ?: "Native tool failed", e.stdout)
        else -> Screen.Error(e.message ?: "Unexpected error: ${e::class.simpleName}")
    }

    fun reset() {
        _selectedNames.value = emptySet()
        _screen.value = Screen.PickFile
        workDir.deleteRecursively()
    }
}
