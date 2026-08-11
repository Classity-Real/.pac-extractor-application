package dev.classityreal.pext

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.classityreal.pext.ui.theme.PExtTheme

/** Single-pane content stops growing past this width — on tablets/foldables the
 *  rest of the screen stays as breathing room instead of every list/button
 *  stretching edge-to-edge. 600dp lines up with Material's compact/medium
 *  window size class boundary. */
private val MaxContentWidth = 600.dp

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
    // Shared by both flows — which ViewModel method to call is decided by the screen we were
    // on when the picker was launched (SelectPartitions = single-file, SelectMultipleFiles = batch).
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
            PExtTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val screen by viewModel.screen.collectAsState()

                    // Center a max-width column so phones stay full-width but tablets/
                    // foldables don't stretch every button and list edge-to-edge.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Box(modifier = Modifier.widthIn(max = MaxContentWidth).fillMaxSize()) {
                            when (val s = screen) {
                                is Screen.Home -> HomeScreen(
                                    onPickFile = { pickPacFile.launch(arrayOf("*/*")) },
                                    onPickFromList = { pickMultiplePacFiles.launch(arrayOf("*/*")) }
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
                            }
                        }
                    }
                }
            }
        }
    }
}
