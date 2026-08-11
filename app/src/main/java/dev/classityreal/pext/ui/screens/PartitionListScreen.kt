package dev.classityreal.pext.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.classityreal.pext.native.ExtractionEngine
import dev.classityreal.pext.native.NativeEntry
import dev.classityreal.pext.ui.PacViewModel

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartitionListScreen(
    entries: List<NativeEntry>,
    viewModel: PacViewModel,
    onExtractClick: () -> Unit
) {
    val selected by viewModel.selectedNames.collectAsState()
    val engine by viewModel.engine.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Firmware contents") }) },
        floatingActionButton = {
            val label = if (engine == ExtractionEngine.PAC_EXTRACTOR) {
                "Extract all (${entries.size})"
            } else {
                "Extract ${selected.size} partition${if (selected.size == 1) "" else "s"}"
            }
            ExtendedFloatingActionButton(
                text = { Text(label) },
                icon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                onClick = onExtractClick
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            EngineSelector(engine = engine, onEngineChange = viewModel::setEngine)

            if (engine == ExtractionEngine.UNPAC) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { viewModel.selectAll(entries) }) { Text("Select all") }
                    TextButton(onClick = { viewModel.selectNone() }) { Text("Select none") }
                }
            } else {
                Text(
                    "pacextractor always extracts every partition — switch to unpac to pick individual ones.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // GridCells.Adaptive gives 1 column on phone-width screens (looks identical
            // to the old plain list) and automatically adds columns as width grows past
            // ~260dp per cell — i.e. a real grid on tablets, without a manual breakpoint.
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 260.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries, key = { it.name }) { entry ->
                    PartitionRow(
                        entry = entry,
                        selectable = engine == ExtractionEngine.UNPAC,
                        checked = entry.name in selected,
                        onCheckedChange = { viewModel.toggleEntry(entry.name) }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(64.dp)) }
            }
        }
    }
}

@Composable
private fun EngineSelector(engine: ExtractionEngine, onEngineChange: (ExtractionEngine) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = engine == ExtractionEngine.UNPAC,
            onClick = { onEngineChange(ExtractionEngine.UNPAC) },
            label = { Text("unpac (choose partitions)") }
        )
        FilterChip(
            selected = engine == ExtractionEngine.PAC_EXTRACTOR,
            onClick = { onEngineChange(ExtractionEngine.PAC_EXTRACTOR) },
            label = { Text("pacextractor (extract all)") }
        )
    }
}

@Composable
private fun PartitionRow(entry: NativeEntry, selectable: Boolean, checked: Boolean, onCheckedChange: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectable) {
                Checkbox(checked = checked, onCheckedChange = { onCheckedChange() })
            }
            Column(modifier = Modifier.padding(start = if (selectable) 8.dp else 16.dp, top = 8.dp, bottom = 8.dp)) {
                Text(entry.name.ifBlank { "(unnamed)" }, style = MaterialTheme.typography.bodyLarge)
                Text(
                    formatSize(entry.size) + if (entry.id.isNotBlank()) " · ${entry.id}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
