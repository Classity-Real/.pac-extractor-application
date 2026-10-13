package dev.classityreal.pext

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import dev.classityreal.pext.ui.PacViewModel
import dev.classityreal.pext.ui.Screen
import dev.classityreal.pext.ui.screens.BatchDoneScreen
import dev.classityreal.pext.ui.screens.BatchExtractingScreen
import dev.classityreal.pext.ui.screens.DoneScreen
import dev.classityreal.pext.ui.screens.ErrorScreen
import dev.classityreal.pext.ui.screens.ExtractingScreen
import dev.classityreal.pext.ui.screens.HomeScreen
import dev.classityreal.pext.ui.screens.LoadingScreen
import dev.classityreal.pext.ui.screens.PartitionListScreen
import dev.classityreal.pext.ui.screens.SelectMultipleFilesScreen
import dev.classityreal.pext.ui.screens.SettingsScreen
import dev.classityreal.pext.ui.theme.PExtTheme

/** Single-pane content (Home, progress screens, etc.) stops growing past this width —
 *  on tablets/foldables the rest of the screen stays as breathing room instead of every
 *  button/status text stretching edge-to-edge. 600dp lines up with Material's
 *  compact/medium window size class boundary. */
private val NarrowContentMaxWidth = 600.dp

/** The two grid screens (partition list, batch file list) get much more room instead —
 *  otherwise capping at 600dp would only ever leave space for ~2 grid columns even on a
 *  large tablet. 1200dp is a generous ceiling, not a real constraint on any current device. */
private val GridContentMaxWidth = 1200.dp

private fun maxContentWidthFor(screen: Screen): Dp = when (screen) {
    is Screen.SelectPartitions, is Screen.SelectMultipleFiles -> GridContentMaxWidth
    is Screen.Settings -> 720.dp
    else -> NarrowContentMaxWidth
}

class MainActivity : ComponentActivity() {

    private val viewModel: PacViewModel by viewModels()

    // Step 1 (single-file flow): user picks the .pac file itself.
    private val pickPacFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.onPacFilePicked(it)
        }
    }

    // Step 1 (batch flow): user picks several .pac files at once ("Select from list").
    private val pickMultiplePacFiles = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            viewModel.onMultipleFilesPicked(uris)
        }
    }

    // Step 2: after choosing what to extract, user grants access to a destination folder.
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

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            val settings by viewModel.settings.collectAsState()
            val darkTheme = when (settings.preferDarkTheme) {
                true -> true
                false -> false
                null -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            PExtTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.useDynamicColor
            ) {
                // Back panel / secondary surface: outer Material surface fills the
                // tablet so the centered content panel sits on a distinct layer
                // (Material 3 "back panel" style breathing room on large screens).
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest
                ) {
                    val screen by viewModel.screen.collectAsState()

                    BackHandler(enabled = screen is Screen.Settings) {
                        viewModel.closeSettings()
                    }
                    BackHandler(enabled = screen !is Screen.Home && screen !is Screen.Settings &&
                        screen !is Screen.Extracting && screen !is Screen.BatchExtracting) {
                        viewModel.reset()
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Surface(
                            modifier = Modifier
                                .widthIn(max = maxContentWidthFor(screen))
                                .fillMaxSize(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            when (val s = screen) {
                                is Screen.Home -> HomeScreen(
                                    onPickFile = { pickPacFile.launch(arrayOf("*/*")) },
                                    onPickFromList = { pickMultiplePacFiles.launch(arrayOf("*/*")) },
                                    onOpenSettings = { viewModel.openSettings() },
                                    useExpressiveBackground = settings.useExpressiveBackground
                                )

                                is Screen.Loading -> LoadingScreen()

                                is Screen.SelectPartitions -> PartitionListScreen(
                                    entries = s.entries,
                                    viewModel = viewModel,
                                    onExtractClick = { pickOutputTree.launch(null) }
                                )

                                is Screen.Extracting -> ExtractingScreen(engine = s.engine, lastLine = s.lastLine)

                                is Screen.Done -> DoneScreen(onDoneClick = { viewModel.reset() })

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

                                is Screen.BatchDone -> BatchDoneScreen(
                                    folderNames = s.folderNames,
                                    onDoneClick = { viewModel.reset() }
                                )

                                is Screen.Error -> ErrorScreen(
                                    message = s.message,
                                    detail = s.detail,
                                    onRetryClick = { viewModel.reset() }
                                )

                                is Screen.Settings -> SettingsScreen(
                                    state = settings,
                                    onBack = { viewModel.closeSettings() },
                                    onToggleDynamicColor = viewModel::setUseDynamicColor,
                                    onToggleExpressiveBackground = viewModel::setUseExpressiveBackground,
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
