package dev.classityreal.pext

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.classityreal.pext.ui.MainTab
import dev.classityreal.pext.ui.PacViewModel
import dev.classityreal.pext.ui.Screen
import dev.classityreal.pext.ui.screens.BatchDoneScreen
import dev.classityreal.pext.ui.screens.BatchExtractingScreen
import dev.classityreal.pext.ui.screens.ConsoleScreen
import dev.classityreal.pext.ui.screens.DoneScreen
import dev.classityreal.pext.ui.screens.ErrorScreen
import dev.classityreal.pext.ui.screens.ExtractingScreen
import dev.classityreal.pext.ui.screens.FilesScreen
import dev.classityreal.pext.ui.screens.HomeScreen
import dev.classityreal.pext.ui.screens.LoadingScreen
import dev.classityreal.pext.ui.screens.PartitionListScreen
import dev.classityreal.pext.ui.screens.SelectMultipleFilesScreen
import dev.classityreal.pext.ui.screens.SettingsScreen
import dev.classityreal.pext.ui.theme.PExtTheme

private val NarrowContentMaxWidth = 600.dp
private val GridContentMaxWidth = 1200.dp

private fun maxContentWidthFor(screen: Screen): Dp = when (screen) {
    is Screen.SelectPartitions, is Screen.SelectMultipleFiles -> GridContentMaxWidth
    is Screen.Settings -> 720.dp
    else -> NarrowContentMaxWidth
}

class MainActivity : ComponentActivity() {

    private val viewModel: PacViewModel by viewModels()

    private val pickPacFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.onPacFilePicked(it)
        }
    }

    private val pickMultiplePacFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.onMultipleFilesPicked(uris)
        }
    }

    private val pickOutputTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { treeUri ->
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            when (val s = viewModel.screen.value) {
                is Screen.SelectPartitions -> viewModel.startExtraction(treeUri)
                is Screen.SelectMultipleFiles -> viewModel.startBatchExtraction(s.files, treeUri)
                else -> Unit
            }
        }
    }

    private val pickBrowseFolder = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { viewModel.onBrowseFolderPicked(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val darkTheme = when (settings.preferDarkTheme) {
                true -> true
                false -> false
                null -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            // Always monochrome — dynamic color disabled
            PExtTheme(darkTheme = darkTheme, dynamicColor = false) {
                val screen by viewModel.screen.collectAsState()
                val mainTab by viewModel.mainTab.collectAsState()
                val showBottomBar = screen is Screen.Home || screen is Screen.Done ||
                    screen is Screen.BatchDone || screen is Screen.Error

                BackHandler(enabled = screen is Screen.Settings) { viewModel.closeSettings() }
                BackHandler(
                    enabled = screen !is Screen.Home && screen !is Screen.Settings &&
                        screen !is Screen.Extracting && screen !is Screen.BatchExtracting
                ) { viewModel.reset() }

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (showBottomBar && screen !is Screen.Settings) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = mainTab == MainTab.Home,
                                    onClick = {
                                        viewModel.setMainTab(MainTab.Home)
                                        if (screen !is Screen.Home) viewModel.reset()
                                    },
                                    icon = {
                                        Icon(
                                            if (mainTab == MainTab.Home) Icons.Filled.Home
                                            else Icons.Outlined.Home,
                                            contentDescription = "Home"
                                        )
                                    },
                                    label = { Text("Home") }
                                )
                                NavigationBarItem(
                                    selected = mainTab == MainTab.Files,
                                    onClick = { viewModel.setMainTab(MainTab.Files) },
                                    icon = {
                                        Icon(
                                            if (mainTab == MainTab.Files) Icons.Filled.Folder
                                            else Icons.Outlined.Folder,
                                            contentDescription = "Files"
                                        )
                                    },
                                    label = { Text("Files") }
                                )
                                NavigationBarItem(
                                    selected = mainTab == MainTab.Console,
                                    onClick = { viewModel.setMainTab(MainTab.Console) },
                                    icon = {
                                        Icon(
                                            if (mainTab == MainTab.Console) Icons.Filled.Terminal
                                            else Icons.Outlined.Terminal,
                                            contentDescription = "Console"
                                        )
                                    },
                                    label = { Text("Console") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .windowInsetsPadding(WindowInsets.statusBars),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = maxContentWidthFor(screen))
                                .fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            when (val s = screen) {
                                is Screen.Home, is Screen.Done, is Screen.BatchDone, is Screen.Error -> {
                                    when (mainTab) {
                                        MainTab.Home -> when (s) {
                                            is Screen.Home -> HomeScreen(
                                                onPickFile = { pickPacFile.launch(arrayOf("*/*")) },
                                                onPickFromList = { pickMultiplePacFiles.launch(arrayOf("*/*")) },
                                                onOpenSettings = { viewModel.openSettings() },
                                                updateAvailable = settings.updateAvailable,
                                                currentVersion = settings.currentVersion,
                                                latestVersion = settings.latestVersion,
                                                onUpdateClick = { viewModel.openUpdate() }
                                            )
                                            is Screen.Done -> DoneScreen(onDoneClick = { viewModel.reset() })
                                            is Screen.BatchDone -> BatchDoneScreen(
                                                folderNames = s.folderNames,
                                                onDoneClick = { viewModel.reset() }
                                            )
                                            is Screen.Error -> ErrorScreen(
                                                message = s.message,
                                                detail = s.detail,
                                                onRetryClick = { viewModel.reset() }
                                            )
                                            else -> {}
                                        }
                                        MainTab.Files -> {
                                            val files by viewModel.managedFiles.collectAsState()
                                            val label by viewModel.folderLabel.collectAsState()
                                            FilesScreen(
                                                files = files,
                                                folderLabel = label,
                                                onOpenFolder = { pickBrowseFolder.launch(null) },
                                                onRefresh = { viewModel.refreshFolder() }
                                            )
                                        }
                                        MainTab.Console -> {
                                            val lines by viewModel.console.collectAsState()
                                            ConsoleScreen(
                                                lines = lines,
                                                onClear = { viewModel.clearConsole() }
                                            )
                                        }
                                    }
                                }

                                is Screen.Loading -> LoadingScreen()

                                is Screen.SelectPartitions -> PartitionListScreen(
                                    entries = s.entries,
                                    viewModel = viewModel,
                                    onExtractClick = { pickOutputTree.launch(null) }
                                )

                                is Screen.Extracting -> ExtractingScreen(engine = s.engine, lastLine = s.lastLine)

                                is Screen.SelectMultipleFiles -> SelectMultipleFilesScreen(
                                    files = s.files,
                                    freeSpaceBytes = viewModel.freeSpaceBytes(),
                                    onExtractClick = { pickOutputTree.launch(null) }
                                )

                                is Screen.BatchExtracting -> BatchExtractingScreen(
                                    currentIndex = s.currentIndex,
                                    total = s.total,
                                    currentName = s.currentName,
                                    lastLine = s.lastLine
                                )

                                is Screen.Settings -> SettingsScreen(
                                    state = settings,
                                    onBack = { viewModel.closeSettings() },
                                    onThemeModeChange = viewModel::setThemeMode,
                                    onPreferUnpacChange = viewModel::setPreferUnpac,
                                    onUpdateClick = viewModel::openUpdate
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
