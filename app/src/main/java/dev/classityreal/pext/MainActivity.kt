package dev.classityreal.pext

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.classityreal.pext.ui.PacViewModel
import dev.classityreal.pext.ui.Screen
import dev.classityreal.pext.ui.screens.DoneScreen
import dev.classityreal.pext.ui.screens.ErrorScreen
import dev.classityreal.pext.ui.screens.ExtractingScreen
import dev.classityreal.pext.ui.screens.FilePickerScreen
import dev.classityreal.pext.ui.screens.LoadingScreen
import dev.classityreal.pext.ui.screens.PartitionListScreen
import dev.classityreal.pext.ui.theme.PExtTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PacViewModel by viewModels()

    // Step 1: user picks the .pac file itself.
    private val pickPacFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.onPacFilePicked(it)
        }
    }

    // Step 2: after choosing partitions, user picks an output folder to write into.
    private val pickOutputTree = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let { treeUri ->
            contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            viewModel.startExtraction(treeUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PExtTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val screen by viewModel.screen.collectAsState()

                    when (val s = screen) {
                        is Screen.PickFile -> FilePickerScreen(
                            onPickFile = { pickPacFile.launch(arrayOf("*/*")) }
                        )

                        is Screen.Loading -> LoadingScreen()

                        is Screen.SelectPartitions -> PartitionListScreen(
                            entries = s.entries,
                            viewModel = viewModel,
                            onExtractClick = { pickOutputTree.launch(null) }
                        )

                        is Screen.Extracting -> ExtractingScreen(engine = s.engine, lastLine = s.lastLine)

                        is Screen.Done -> DoneScreen(onDoneClick = { viewModel.reset() })

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
