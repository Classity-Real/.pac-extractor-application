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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.classityreal.pext.native.ExtractionEngine
import dev.classityreal.pext.native.NativeEntry
import dev.classityreal.pext.ui.PacViewModel

private val CardRadius = 20.dp

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
    onExtractClick: () -> Unit,
    onBack: () -> Unit = {}
) {
    val selected by viewModel.selectedNames.collectAsState()
    val engine by viewModel.engine.collectAsState()
    val canExtract = engine == ExtractionEngine.PAC_EXTRACTOR || selected.isNotEmpty()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Select partitions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            SurfaceBar {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        if (engine == ExtractionEngine.UNPAC)
                            "${selected.size} of ${entries.size} selected"
                        else
                            "All ${entries.size} partitions",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onExtractClick,
                        enabled = canExtract,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Icon(Icons.Filled.FileDownload, null, Modifier.size(20.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            if (engine == ExtractionEngine.PAC_EXTRACTOR)
                                "Extract all"
                            else
                                "Extract ${selected.size}"
                        )
                    }
                }
            }
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

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entries, key = { it.name }) { entry ->
                    PartitionCard(
                        entry = entry,
                        selectable = engine == ExtractionEngine.UNPAC,
                        checked = entry.name in selected,
                        onToggle = { viewModel.toggleEntry(entry.name) }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun SurfaceBar(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        content()
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
            label = { Text("unpac") }
        )
        FilterChip(
            selected = engine == ExtractionEngine.PAC_EXTRACTOR,
            onClick = { onEngineChange(ExtractionEngine.PAC_EXTRACTOR) },
            label = { Text("pacextractor (all)") }
        )
    }
}

@Composable
private fun PartitionCard(
    entry: NativeEntry,
    selectable: Boolean,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = { if (selectable) onToggle() },
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (checked && selectable)
                MaterialTheme.colorScheme.surfaceContainerHighest
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectable) {
                Checkbox(checked = checked, onCheckedChange = { onToggle() })
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = if (selectable) 4.dp else 8.dp)
            ) {
                Text(
                    entry.name.ifBlank { "(unnamed)" },
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    buildString {
                        append(formatSize(entry.size))
                        if (entry.id.isNotBlank()) append(" · ").append(entry.id)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
