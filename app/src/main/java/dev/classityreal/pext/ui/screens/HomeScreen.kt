package dev.classityreal.pext.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val AnimMs = 500
private val CardRadius = 28.dp

/** Fade + slide up, same 0.5s duration used for every staged item. */
private fun enterAnim(delayMs: Int) = fadeIn(
    animationSpec = tween(durationMillis = AnimMs, delayMillis = delayMs)
) + slideInVertically(
    animationSpec = tween(durationMillis = AnimMs, delayMillis = delayMs),
    initialOffsetY = { it / 4 }
)

@Composable
fun HomeScreen(
    onPickFile: () -> Unit,
    onPickFromList: () -> Unit,
    onOpenSettings: () -> Unit = {},
    useExpressiveBackground: Boolean = false,
    updateAvailable: Boolean = false,
    currentVersion: String = "",
    latestVersion: String? = null,
    onUpdateClick: () -> Unit = {}
) {
    // Stagger visibility: each step waits 0.5s then reveals the next block
    var showBanner by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showPrimary by remember { mutableStateOf(false) }
    var showSecondary by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showBanner = true
        delay(AnimMs.toLong())
        if (updateAvailable && latestVersion != null) {
            showUpdate = true
            delay(AnimMs.toLong())
        }
        showTitle = true
        delay(AnimMs.toLong())
        showPrimary = true
        delay(AnimMs.toLong())
        showSecondary = true
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Banner always on top — Android 15 style full-width card
                AnimatedVisibility(visible = showBanner, enter = enterAnim(0)) {
                    BrandBannerCard()
                }

                if (updateAvailable && latestVersion != null) {
                    Spacer(Modifier.height(12.dp))
                    AnimatedVisibility(visible = showUpdate, enter = enterAnim(0)) {
                        UpdateActionCard(
                            currentVersion = currentVersion,
                            latestVersion = latestVersion,
                            onUpdateClick = onUpdateClick
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                AnimatedVisibility(visible = showTitle, enter = enterAnim(0)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Extract Unisoc .pac firmware",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Pick one .pac file to inspect and choose partitions, or select several for batch extract.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Grouped actions like Android 15 Settings sections
                AnimatedVisibility(visible = showPrimary, enter = enterAnim(0)) {
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
                            Icon(
                                Icons.Filled.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Select a file",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Inspect partitions and extract a subset",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(visible = showSecondary, enter = enterAnim(0)) {
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
                            Icon(
                                Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.size(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Select from list",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Batch-extract several .pac files",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

/** Always-visible brand banner card (Android 15 rounded surface). */
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
                        moveTo(w * 0.35f, 0f)
                        lineTo(w * 0.55f, 0f)
                        lineTo(w * 0.42f, h)
                        lineTo(w * 0.22f, h)
                        close()
                    },
                    color = Color(0xFF333333)
                )
                drawPath(
                    Path().apply {
                        moveTo(w * 0.55f, 0f)
                        lineTo(w * 0.78f, 0f)
                        lineTo(w * 0.65f, h)
                        lineTo(w * 0.42f, h)
                        close()
                    },
                    color = Color(0xFF3A3A3A)
                )
                val cx = w * 0.48f
                val cy = h * 0.5f
                drawPath(
                    Path().apply {
                        moveTo(cx - 18f, cy - 22f)
                        lineTo(cx + 6f, cy)
                        lineTo(cx - 18f, cy + 22f)
                        lineTo(cx - 10f, cy + 22f)
                        lineTo(cx + 14f, cy)
                        lineTo(cx - 10f, cy - 22f)
                        close()
                    },
                    color = Color(0x22FFFFFF)
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

/** Separate update action card under the banner when a newer release exists. */
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.SystemUpdate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Update available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
            ) {
                Text("Update")
            }
        }
    }
}
