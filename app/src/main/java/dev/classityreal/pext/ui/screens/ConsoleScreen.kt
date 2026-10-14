package dev.classityreal.pext.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ConsoleLevel { INFO, OK, WARN, ERR }

data class ConsoleLine(
    val level: ConsoleLevel,
    val message: String
)

@Composable
fun ConsoleScreen(
    lines: List<ConsoleLine>,
    onClear: () -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Console",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Tool output and status",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(onClick = onClear) { Text("Clear") }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (lines.isEmpty()) {
                Text(
                    "[-] Ready. Extraction logs appear here.",
                    color = Color(0xFF9E9E9E),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    items(lines) { line ->
                        val (tag, color) = when (line.level) {
                            ConsoleLevel.INFO -> "[-]" to Color(0xFFB0B0B0)
                            ConsoleLevel.OK -> "[+]" to Color(0xFF81C784)
                            ConsoleLevel.WARN -> "[!]" to Color(0xFFFFB74D)
                            ConsoleLevel.ERR -> "[x]" to Color(0xFFE57373)
                        }
                        Row(modifier = Modifier.padding(vertical = 2.dp)) {
                            Text(
                                tag,
                                color = color,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                line.message,
                                color = Color(0xFFE0E0E0),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
