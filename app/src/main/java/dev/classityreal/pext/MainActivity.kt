package dev.classityreal.pext

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import dev.classityreal.pext.ui.screens.SettingsState
import dev.classityreal.pext.ui.theme.PExtTheme

private val TabletBreakpoint = 600.dp
private val SettingsPaneWidth = 360.dp

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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.parseColor("#1C1C1C")),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.parseColor("#1C1C1C"))
        )
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsState()

            PExtTheme(darkTheme = true, dynamicColor = false) {
                val screen by viewModel.screen.collectAsState()
                val mainTab by viewModel.mainTab.collectAsState()
                var showConsoleSheet by remember { mutableStateOf(false) }
                val consoleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                val inFlow = screen is Screen.Loading ||
                    screen is Screen.SelectPartitions ||
                    screen is Screen.Extracting ||
                    screen is Screen.SelectMultipleFiles ||
                    screen is Screen.BatchExtracting

                BackHandler(enabled = screen is Screen.Settings) { viewModel.closeSettings() }
                BackHandler(enabled = showConsoleSheet) { showConsoleSheet = false }
                BackHandler(
                    enabled = !showConsoleSheet && screen !is Screen.Home && screen !is Screen.Settings &&
                        screen !is Screen.Extracting && screen !is Screen.BatchExtracting
                ) { viewModel.reset() }

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    val isTablet = maxWidth >= TabletBreakpoint
                    val showSettingsPane = isTablet && screen is Screen.Settings

                    if (isTablet) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .windowInsetsPadding(WindowInsets.statusBars)
                        ) {
                            if (!inFlow) {
                                AppRail(
                                    mainTab = mainTab,
                                    consoleOpen = showConsoleSheet,
                                    onHome = {
                                        showConsoleSheet = false
                                        viewModel.setMainTab(MainTab.Home)
                                        if (screen !is Screen.Home) viewModel.reset()
                                    },
                                    onFiles = {
                                        showConsoleSheet = false
                                        viewModel.setMainTab(MainTab.Files)
                                        if (screen is Screen.Settings) viewModel.closeSettings()
                                    },
                                    onConsole = { showConsoleSheet = true }
                                )
                            }

                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MainContent(
                                    screen = if (showSettingsPane) Screen.Home else screen,
                                    mainTab = mainTab,
                                    settings = settings,
                                    isTablet = true,
                                    onPickFile = { pickPacFile.launch(arrayOf("*/*")) },
                                    onPickFromList = { pickMultiplePacFiles.launch(arrayOf("*/*")) },
                                    onOpenSettings = { viewModel.openSettings() },
                                    onExtract = { pickOutputTree.launch(null) },
                                    onBrowseFolder = { pickBrowseFolder.launch(null) }
                                )
                            }

                            if (showSettingsPane) {
                                Surface(
                                    modifier = Modifier
                                        .width(SettingsPaneWidth)
                                        .fillMaxHeight(),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp
                                ) {
                                    SettingsScreen(
                                        state = settings,
                                        onBack = { viewModel.closeSettings() },
                                        onThemeModeChange = viewModel::setThemeMode,
                                        onPreferUnpacChange = viewModel::setPreferUnpac,
                                        onUpdateClick = viewModel::openUpdate
                                    )
                                }
                            }
                        }
                    } else {
                        Scaffold(
                            containerColor = MaterialTheme.colorScheme.background,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            bottomBar = {
                                if (!inFlow && screen !is Screen.Settings) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .windowInsetsPadding(WindowInsets.navigationBars)
                                            .padding(horizontal = 24.dp, vertical = 12.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(28.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            tonalElevation = 3.dp,
                                            shadowElevation = 6.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            FloatingNav(
                                                mainTab = mainTab,
                                                consoleOpen = showConsoleSheet,
                                                onHome = {
                                                    showConsoleSheet = false
                                                    viewModel.setMainTab(MainTab.Home)
                                                    if (screen !is Screen.Home) viewModel.reset()
                                                },
                                                onFiles = {
                                                    showConsoleSheet = false
                                                    viewModel.setMainTab(MainTab.Files)
                                                },
                                                onConsole = { showConsoleSheet = true }
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .windowInsetsPadding(WindowInsets.statusBars)
                            ) {
                                MainContent(
                                    screen = screen,
                                    mainTab = mainTab,
                                    settings = settings,
                                    isTablet = false,
                                    onPickFile = { pickPacFile.launch(arrayOf("*/*")) },
                                    onPickFromList = { pickMultiplePacFiles.launch(arrayOf("*/*")) },
                                    onOpenSettings = { viewModel.openSettings() },
                                    onExtract = { pickOutputTree.launch(null) },
                                    onBrowseFolder = { pickBrowseFolder.launch(null) }
                                )
                            }
                        }
                    }

                    if (showConsoleSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showConsoleSheet = false },
                            sheetState = consoleSheetState,
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            val lines by viewModel.console.collectAsState()
                            Box(modifier = Modifier.height(420.dp)) {
                                ConsoleScreen(
                                    lines = lines,
                                    onClear = { viewModel.clearConsole() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AppRail(
        mainTab: MainTab,
        consoleOpen: Boolean,
        onHome: () -> Unit,
        onFiles: () -> Unit,
        onConsole: () -> Unit
    ) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxHeight()
        ) {
            Spacer(Modifier.height(12.dp))
            NavigationRailItem(
                selected = mainTab == MainTab.Home && !consoleOpen,
                onClick = onHome,
                icon = {
                    Icon(
                        if (mainTab == MainTab.Home) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home"
                    )
                },
                label = { Text("Home") }
            )
            NavigationRailItem(
                selected = mainTab == MainTab.Files && !consoleOpen,
                onClick = onFiles,
                icon = {
                    Icon(
                        if (mainTab == MainTab.Files) Icons.Filled.Folder else Icons.Outlined.Folder,
                        contentDescription = "Files"
                    )
                },
                label = { Text("Files") }
            )
            NavigationRailItem(
                selected = consoleOpen,
                onClick = onConsole,
                icon = {
                    Icon(
                        if (consoleOpen) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                        contentDescription = "Console"
                    )
                },
                label = { Text("Console") }
            )
        }
    }

    @Composable
    private fun FloatingNav(
        mainTab: MainTab,
        consoleOpen: Boolean,
        onHome: () -> Unit,
        onFiles: () -> Unit,
        onConsole: () -> Unit
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = mainTab == MainTab.Home && !consoleOpen,
                onClick = onHome,
                icon = {
                    Icon(
                        if (mainTab == MainTab.Home) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home"
                    )
                },
                label = { Text("Home") }
            )
            NavigationBarItem(
                selected = mainTab == MainTab.Files && !consoleOpen,
                onClick = onFiles,
                icon = {
                    Icon(
                        if (mainTab == MainTab.Files) Icons.Filled.Folder else Icons.Outlined.Folder,
                        contentDescription = "Files"
                    )
                },
                label = { Text("Files") }
            )
            NavigationBarItem(
                selected = consoleOpen,
                onClick = onConsole,
                icon = {
                    Icon(
                        if (consoleOpen) Icons.Filled.Terminal else Icons.Outlined.Terminal,
                        contentDescription = "Console"
                    )
                },
                label = { Text("Console") }
            )
        }
    }

    @Composable
    private fun MainContent(
        screen: Screen,
        mainTab: MainTab,
        settings: SettingsState,
        isTablet: Boolean,
        onPickFile: () -> Unit,
        onPickFromList: () -> Unit,
        onOpenSettings: () -> Unit,
        onExtract: () -> Unit,
        onBrowseFolder: () -> Unit
    ) {
        when (val s = screen) {
            is Screen.Home, is Screen.Done, is Screen.BatchDone, is Screen.Error -> {
                when (mainTab) {
                    MainTab.Home -> when (s) {
                        is Screen.Home -> HomeScreen(
                            onPickFile = onPickFile,
                            onPickFromList = onPickFromList,
                            onOpenSettings = onOpenSettings,
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
                    MainTab.Files, MainTab.Console -> {
                        val files by viewModel.managedFiles.collectAsState()
                        val label by viewModel.folderLabel.collectAsState()
                        FilesScreen(
                            files = files,
                            folderLabel = label,
                            onOpenFolder = onBrowseFolder,
                            onRefresh = { viewModel.refreshFolder() }
                        )
                    }
                }
            }

            is Screen.Loading -> LoadingScreen()

            is Screen.SelectPartitions -> PartitionListScreen(
                entries = s.entries,
                viewModel = viewModel,
                onExtractClick = onExtract,
                onBack = { viewModel.reset() }
            )

            is Screen.Extracting -> ExtractingScreen(engine = s.engine, lastLine = s.lastLine)

            is Screen.SelectMultipleFiles -> SelectMultipleFilesScreen(
                files = s.files,
                freeSpaceBytes = viewModel.freeSpaceBytes(),
                onExtractClick = onExtract
            )

            is Screen.BatchExtracting -> BatchExtractingScreen(
                currentIndex = s.currentIndex,
                total = s.total,
                currentName = s.currentName,
                lastLine = s.lastLine
            )

            is Screen.Settings -> {
                if (!isTablet) {
                    SettingsScreen(
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
