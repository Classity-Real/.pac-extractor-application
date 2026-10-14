package dev.classityreal.pext.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

data class SettingsState(
    val useDynamicColor: Boolean = true,
    val useExpressiveBackground: Boolean = true,
    val preferDarkTheme: Boolean? = null,
    val preferUnpacEngine: Boolean = true,
    val currentVersion: String = "0.1.0",
    val latestVersion: String? = null,
    val updateAvailable: Boolean = false
)

private const val AnimMs = 500
private val CardRadius = 28.dp

private fun enterAnim() = fadeIn(
    animationSpec = tween(durationMillis = AnimMs)
) + slideInVertically(
    animationSpec = tween(durationMillis = AnimMs),
    initialOffsetY = { it / 5 }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsState,
    onBack: () -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onToggleExpressiveBackground: (Boolean) -> Unit,
    onThemeModeChange: (Boolean?) -> Unit,
    onPreferUnpacChange: (Boolean) -> Unit,
    onUpdateClick: () -> Unit
) {
    var showAppearance by remember { mutableStateOf(false) }
    var showExtraction by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showUpdateBtn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showAppearance = true
        delay(AnimMs.toLong())
        showExtraction = true
        delay(AnimMs.toLong())
        showAbout = true
        delay(AnimMs.toLong())
        if (state.updateAvailable) showUpdateBtn = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimatedVisibility(visible = showAppearance, enter = enterAnim()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Appearance",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    // Android 15: one rounded group card for related toggles
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardRadius),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text("Dynamic color (Material You)") },
                            supportingContent = { Text("Match system wallpaper colors on Android 12+") },
                            leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = state.useDynamicColor,
                                    onCheckedChange = onToggleDynamicColor
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            headlineContent = { Text("Expressive launch background") },
                            supportingContent = { Text("Gradient / tinted surface when the app opens") },
                            leadingContent = { Icon(Icons.Filled.Tune, contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = state.useExpressiveBackground,
                                    onCheckedChange = onToggleExpressiveBackground
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        ListItem(
                            headlineContent = { Text("Follow system dark mode") },
                            supportingContent = { Text("Off = always light; On = always dark when system is dark") },
                            leadingContent = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                            trailingContent = {
                                Switch(
                                    checked = state.preferDarkTheme != false,
                                    onCheckedChange = { follow ->
                                        onThemeModeChange(if (follow) null else false)
                                    }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(visible = showExtraction, enter = enterAnim()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Extraction",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardRadius),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text("Prefer unpac (partition picker)") },
                            supportingContent = { Text("Default engine for single-file flow") },
                            trailingContent = {
                                Switch(
                                    checked = state.preferUnpacEngine,
                                    onCheckedChange = onPreferUnpacChange
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(visible = showAbout, enter = enterAnim()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(CardRadius),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        ListItem(
                            headlineContent = { Text("Version") },
                            supportingContent = {
                                Text(
                                    if (state.updateAvailable && state.latestVersion != null) {
                                        "${state.currentVersion} → ${state.latestVersion} available"
                                    } else {
                                        state.currentVersion
                                    }
                                )
                            },
                            leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            if (state.updateAvailable) {
                Spacer(Modifier.height(12.dp))
                AnimatedVisibility(visible = showUpdateBtn, enter = enterAnim()) {
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

            Spacer(Modifier.height(24.dp))
            Text(
                "Customizations apply immediately. Update opens the latest release when available.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
