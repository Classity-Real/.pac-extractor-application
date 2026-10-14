package dev.classityreal.pext.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.classityreal.pext.native.ExtractionEngine
import dev.classityreal.pext.native.NativeEntry
import dev.classityreal.pext.native.NativeToolException
import dev.classityreal.pext.native.PacExtractorRunner
import dev.classityreal.pext.native.SafBridge
import dev.classityreal.pext.native.UnpacRunner
import dev.classityreal.pext.ui.screens.ConsoleLevel
import dev.classityreal.pext.ui.screens.ConsoleLine
import dev.classityreal.pext.ui.screens.ManagedFile
import dev.classityreal.pext.ui.screens.SettingsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface Screen {
    data object Home : Screen
    data object Loading : Screen
    data class SelectPartitions(val entries: List<NativeEntry>) : Screen
    data class Extracting(val engine: ExtractionEngine, val lastLine: String) : Screen
    data class Done(val outputDir: Uri) : Screen

    data class SelectMultipleFiles(val files: List<PickedPac>) : Screen
    data class BatchExtracting(val currentIndex: Int, val total: Int, val currentName: String, val lastLine: String) : Screen
    data class BatchDone(val folderNames: List<String>) : Screen

    data class Error(val message: String, val detail: String = "") : Screen
    data object Settings : Screen
}

data class PickedPac(val uri: Uri, val name: String, val size: Long)

enum class MainTab { Home, Files, Console }

class PacViewModel(app: Application) : AndroidViewModel(app) {

    private val _screen = MutableStateFlow<Screen>(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _mainTab = MutableStateFlow(MainTab.Home)
    val mainTab: StateFlow<MainTab> = _mainTab.asStateFlow()

    private val _selectedNames = MutableStateFlow<Set<String>>(emptySet())
    val selectedNames: StateFlow<Set<String>> = _selectedNames.asStateFlow()

    private val _engine = MutableStateFlow(ExtractionEngine.UNPAC)
    val engine: StateFlow<ExtractionEngine> = _engine.asStateFlow()

    private val _console = MutableStateFlow(
        listOf(ConsoleLine(ConsoleLevel.INFO, "Ready. Extraction logs appear here."))
    )
    val console: StateFlow<List<ConsoleLine>> = _console.asStateFlow()

    private val _managedFiles = MutableStateFlow<List<ManagedFile>>(emptyList())
    val managedFiles: StateFlow<List<ManagedFile>> = _managedFiles.asStateFlow()

    private val _folderLabel = MutableStateFlow<String?>(null)
    val folderLabel: StateFlow<String?> = _folderLabel.asStateFlow()

    private var browseTreeUri: Uri? = null

    private val prefs = app.getSharedPreferences("pext_settings", 0)

    private val _settings = MutableStateFlow(
        SettingsState(
            preferDarkTheme = when (prefs.getString("theme_mode", "system")) {
                "light" -> false
                "dark" -> true
                else -> null
            },
            preferUnpacEngine = prefs.getBoolean("prefer_unpac", true),
            currentVersion = runCatching {
                @Suppress("DEPRECATION")
                app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "0.1.0"
            }.getOrDefault("0.1.0"),
            latestVersion = null,
            updateAvailable = false
        )
    )
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val workDir get() = File(getApplication<Application>().cacheDir, "pac_work")

    private var latestReleaseUrl: String =
        "https://github.com/Classity-Real/.pac-extractor-application/releases"

    init {
        _engine.value = if (_settings.value.preferUnpacEngine) ExtractionEngine.UNPAC
        else ExtractionEngine.PAC_EXTRACTOR
        viewModelScope.launch(Dispatchers.IO) { checkForUpdate() }
        log(ConsoleLevel.OK, "PExt ${_settings.value.currentVersion} started")
    }

    fun setMainTab(tab: MainTab) {
        _mainTab.value = tab
    }

    fun log(level: ConsoleLevel, message: String) {
        _console.update { it + ConsoleLine(level, message) }
    }

    fun clearConsole() {
        _console.value = listOf(ConsoleLine(ConsoleLevel.INFO, "Console cleared."))
    }

    fun onBrowseFolderPicked(uri: Uri) {
        val app = getApplication<Application>()
        browseTreeUri = uri
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                app.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            loadFolder(uri)
        }
    }

    fun refreshFolder() {
        browseTreeUri?.let { uri ->
            viewModelScope.launch(Dispatchers.IO) { loadFolder(uri) }
        }
    }

    private fun loadFolder(uri: Uri) {
        val app = getApplication<Application>()
        val root = DocumentFile.fromTreeUri(app, uri) ?: return
        _folderLabel.value = root.name ?: uri.lastPathSegment
        val listed = root.listFiles().map { doc ->
            ManagedFile(
                name = doc.name ?: "(unnamed)",
                isDirectory = doc.isDirectory,
                sizeLabel = if (doc.isFile) formatSize(doc.length()) else "folder"
            )
        }.sortedWith(compareByDescending<ManagedFile> { it.isDirectory }.thenBy { it.name.lowercase() })
        _managedFiles.value = listed
        log(ConsoleLevel.INFO, "Opened folder: ${_folderLabel.value} (${listed.size} items)")
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var v = bytes.toDouble()
        var u = 0
        while (v >= 1024 && u < units.lastIndex) { v /= 1024; u++ }
        return "%.1f %s".format(v, units[u])
    }

    private suspend fun checkForUpdate() {
        val current = _settings.value.currentVersion
        val result = runCatching {
            val url = URL(
                "https://api.github.com/repos/Classity-Real/.pac-extractor-application/releases/latest"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "PExt-Android")
                connectTimeout = 12_000
                readTimeout = 12_000
            }
            try {
                if (conn.responseCode !in 200..299) return@runCatching null
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.optBoolean("draft", false)) return@runCatching null
                val tag = json.optString("tag_name", "").ifBlank { return@runCatching null }
                val htmlUrl = json.optString("html_url", latestReleaseUrl)
                Triple(tag, htmlUrl, isNewerVersion(tag, current))
            } finally {
                conn.disconnect()
            }
        }.getOrNull()

        if (result != null) {
            val (tag, htmlUrl, newer) = result
            latestReleaseUrl = htmlUrl
            val display = normalizeVersion(tag)
            _settings.update { it.copy(latestVersion = display, updateAvailable = newer) }
            if (newer) log(ConsoleLevel.WARN, "Update available: $current → $display")
        }
    }

    private fun normalizeVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").trim()

    private fun isNewerVersion(remoteRaw: String, localRaw: String): Boolean {
        val remote = parseVersionParts(normalizeVersion(remoteRaw))
        val local = parseVersionParts(normalizeVersion(localRaw))
        val len = maxOf(remote.size, local.size)
        for (i in 0 until len) {
            val r = remote.getOrElse(i) { 0 }
            val l = local.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun parseVersionParts(v: String): List<Int> =
        v.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }.mapNotNull { it.toIntOrNull() }

    fun openSettings() { _screen.value = Screen.Settings }
    fun closeSettings() { _screen.value = Screen.Home }

    fun setThemeMode(dark: Boolean?) {
        val mode = when (dark) {
            true -> "dark"
            false -> "light"
            null -> "system"
        }
        prefs.edit().putString("theme_mode", mode).apply()
        _settings.update { it.copy(preferDarkTheme = dark) }
    }

    fun setPreferUnpac(v: Boolean) {
        prefs.edit().putBoolean("prefer_unpac", v).apply()
        _settings.update { it.copy(preferUnpacEngine = v) }
        _engine.value = if (v) ExtractionEngine.UNPAC else ExtractionEngine.PAC_EXTRACTOR
    }

    fun openUpdate() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(latestReleaseUrl))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun setEngine(e: ExtractionEngine) { _engine.value = e }

    fun onPacFilePicked(uri: Uri) {
        _screen.value = Screen.Loading
        log(ConsoleLevel.INFO, "Loading .pac…")
        viewModelScope.launch {
            val result = runCatching { stageAndList(uri) }
            result.onSuccess { entries ->
                _selectedNames.value = entries.map { it.name }.toSet()
                log(ConsoleLevel.OK, "Listed ${entries.size} partitions")
                _screen.value = Screen.SelectPartitions(entries)
            }.onFailure { e ->
                log(ConsoleLevel.ERR, e.message ?: "List failed")
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
        _selectedNames.update { if (name in it) it - name else it + name }
    }

    fun selectAll(entries: List<NativeEntry>) {
        _selectedNames.value = entries.map { it.name }.toSet()
    }

    fun selectNone() { _selectedNames.value = emptySet() }

    fun startExtraction(outputTreeUri: Uri) {
        val app = getApplication<Application>()
        val localPac = File(workDir, "input.pac")
        val outDir = File(workDir, "out")
        val names = _selectedNames.value.map { it.trim() }.filter { it.isNotEmpty() }
        val chosenEngine = _engine.value

        if (chosenEngine == ExtractionEngine.UNPAC && names.isEmpty()) {
            _screen.value = Screen.Error("No partitions selected")
            return
        }

        log(ConsoleLevel.INFO, "Extracting with $chosenEngine…")
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    outDir.deleteRecursively()
                    when (chosenEngine) {
                        ExtractionEngine.UNPAC -> {
                            UnpacRunner(app).extract(localPac.absolutePath, outDir, names) { line ->
                                log(ConsoleLevel.INFO, line)
                                _screen.value = Screen.Extracting(chosenEngine, line)
                            }
                        }
                        ExtractionEngine.PAC_EXTRACTOR -> {
                            PacExtractorRunner(app).extractAll(localPac.absolutePath, outDir) { line ->
                                log(ConsoleLevel.INFO, line)
                                _screen.value = Screen.Extracting(chosenEngine, line)
                            }
                        }
                    }
                    SafBridge.copyDirToTree(app, outDir, outputTreeUri)
                    workDir.deleteRecursively()
                    outputTreeUri
                }
            }
            result.onSuccess { uri ->
                log(ConsoleLevel.OK, "Extraction complete")
                browseTreeUri = uri
                loadFolder(uri)
                _screen.value = Screen.Done(uri)
            }.onFailure { e ->
                log(ConsoleLevel.ERR, e.message ?: "Extract failed")
                _screen.value = errorFrom(e)
            }
        }
    }

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
                _screen.value = if (files.isEmpty()) Screen.Error("No files were selected")
                else Screen.SelectMultipleFiles(files)
            }.onFailure { e -> _screen.value = errorFrom(e) }
        }
    }

    fun freeSpaceBytes(): Long = SafBridge.internalStorageFreeBytes()

    fun startBatchExtraction(files: List<PickedPac>, outputTreeUri: Uri) {
        val app = getApplication<Application>()
        log(ConsoleLevel.INFO, "Batch extract ${files.size} files…")
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val pextRoot = SafBridge.ensurePExtFolder(app, outputTreeUri)
                    val createdFolderNames = mutableListOf<String>()
                    files.forEachIndexed { index, pac ->
                        val position = index + 1
                        _screen.value = Screen.BatchExtracting(position, files.size, pac.name, "Preparing…")
                        log(ConsoleLevel.INFO, "[$position/${files.size}] ${pac.name}")
                        val batchWorkDir = File(workDir, "batch_$index")
                        batchWorkDir.deleteRecursively()
                        val localPac = SafBridge.copyUriToCache(app, pac.uri, batchWorkDir)
                        val outDir = File(batchWorkDir, "out")
                        outDir.deleteRecursively()
                        PacExtractorRunner(app).extractAll(localPac.absolutePath, outDir) { line ->
                            log(ConsoleLevel.INFO, line)
                            _screen.value = Screen.BatchExtracting(position, files.size, pac.name, line)
                        }
                        val subfolder = SafBridge.createNumberedSubfolder(pextRoot, position)
                        SafBridge.copyDirToDocumentFile(app, outDir, subfolder)
                        createdFolderNames += subfolder.name ?: "Folder $position"
                        batchWorkDir.deleteRecursively()
                    }
                    workDir.deleteRecursively()
                    createdFolderNames
                }
            }
            result.onSuccess { names ->
                log(ConsoleLevel.OK, "Batch done: ${names.joinToString()}")
                _screen.value = Screen.BatchDone(names)
            }.onFailure { e ->
                log(ConsoleLevel.ERR, e.message ?: "Batch failed")
                _screen.value = errorFrom(e)
            }
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
