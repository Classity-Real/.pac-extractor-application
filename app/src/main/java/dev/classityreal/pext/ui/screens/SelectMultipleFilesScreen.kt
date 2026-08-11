package dev.classityreal.pext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.classityreal.pext.ui.PickedPac

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "unknown size"
    val units = arrayOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return "%.1f %s".format(value, units[unit])
}

/**
 * Shown after the user picks multiple .pac files ("Select from list"). Tapping Extract is
 * what actually triggers the folder-access permission flow (ACTION_OPEN_DOCUMENT_TREE) in
 * MainActivity — this screen itself doesn't touch storage yet, it's just a summary + confirm.
 */
@Composable
fun SelectMultipleFilesScreen(
    files: List<PickedPac>,
    freeSpaceBytes: Long,
    onExtractClick: () -> Unit
) {
    val totalSize = files.sumOf { it.size }
    val enoughSpace = freeSpaceBytes > totalSize

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            Text(
                "${files.size} firmware${if (files.size == 1) "" else "s"} selected",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Each firmware will be fully extracted into its own numbered folder " +
                    "(Folder 1, Folder 2, …) inside a PExt folder you choose on internal storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Text("Total size: ${formatSize(totalSize)}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Free space: ${formatSize(freeSpaceBytes)}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (enoughSpace) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
            )
            if (!enoughSpace) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Not enough free space for this batch — free up space before extracting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        // Same adaptive-grid trick as the partition list: single column on phones
        // (looks like the old list), multiple columns once there's tablet-width room.
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 260.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(files) { index, pac ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Folder ${index + 1} — ${pac.name}", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            formatSize(pac.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Button(
            onClick = onExtractClick,
            enabled = enoughSpace,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Extract")
        }
    }
}
