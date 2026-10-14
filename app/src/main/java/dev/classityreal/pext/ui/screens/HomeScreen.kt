package dev.classityreal.pext.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CardRadius = 28.dp

data class CreditEntry(val name: String, val role: String, val icon: ImageVector)

private val Credits = listOf(
    CreditEntry("TomKing062", "Original Binaries from SPDF Code", Icons.Filled.Memory),
    CreditEntry("MaxSteel", "ARM64 Binaries, Maker of SPD Tools for Termux", Icons.Filled.Build),
    CreditEntry("graytvhead", "App Developer and UI Planner", Icons.Filled.Person),
    CreditEntry("Grok Ai Agent", "UI Making and implementions", Icons.Filled.Code),
    CreditEntry("Claude Ai Agent", "Original UI Making", Icons.Filled.Code),
)

@Composable
fun HomeScreen(
    onPickFile: () -> Unit,
    onPickFromList: () -> Unit,
    onOpenSettings: () -> Unit = {},
    updateAvailable: Boolean = false,
    currentVersion: String = "",
    latestVersion: String? = null,
    onUpdateClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }BannerCard()

        if (updateAvailable && latestVersion != null) {
            Spacer(Modifier.height(12.dp))
            UpdateActionCard(
                currentVersion = currentVersion,
                latestVersion = latestVersion,
                onUpdateClick = onUpdateClick
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            "Extract Unisoc .pac firmware",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Pick one .pac file to inspect and choose partitions, or select several for batch extract.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Card(
            onClick = onPickFile,
            shape = RoundedCornerShape(CardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.FolderOpen, null, Modifier.size(28.dp), MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(16.dp))
                Column {
                    Text("Select a file", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Inspect partitions and extract a subset",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            onClick = onPickFromList,
            shape = RoundedCornerShape(CardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Inventory2, null, Modifier.size(28.dp), MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.size(16.dp))
                Column {
                    Text("Select from list", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Batch-extract several .pac files",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Credits on this Tool",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(CardRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Credits.forEachIndexed { index, credit ->
                if (index > 0) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                ListItem(
                    headlineContent = { Text(credit.name) },
                    supportingContent = { Text(credit.role) },
                    leadingContent = {
                        Icon(
                            credit.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BrandBannerCard() {
    Card(
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawPath(
                    Path().apply {
                        moveTo(w * 0.35f, 0f); lineTo(w * 0.55f, 0f)
                        lineTo(w * 0.42f, h); lineTo(w * 0.22f, h); close()
                    },
                    color = Color(0xFF333333)
                )
                drawPath(
                    Path().apply {
                        moveTo(w * 0.55f, 0f); lineTo(w * 0.78f, 0f)
                        lineTo(w * 0.65f, h); lineTo(w * 0.42f, h); close()
                    },
                    color = Color(0xFF3A3A3A)
                )
            }
            Text(
                text = ".pac_extractor",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun UpdateActionCard(
    currentVersion: String,
    latestVersion: String,
    onUpdateClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.SystemUpdate, null, Modifier.size(22.dp))
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("Update available", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$currentVersion → $latestVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onUpdateClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onSurface,
                    contentColor = MaterialTheme.colorScheme.surface
                )
            ) { Text("Update") }
        }
    }
}
