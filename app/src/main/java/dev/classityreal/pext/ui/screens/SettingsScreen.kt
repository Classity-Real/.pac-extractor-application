package dev.classityreal.pext.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class SettingsState(
    val useDynamicColor: Boolean = true,
    val useExpressiveBackground: Boolean = true,
    val preferDarkTheme: Boolean? = null, // null = system
    val preferUnpacEngine: Boolean = true,
    val currentVersion: String = "0.1.0",
    val latestVersion: String? = null,
    val updateAvailable: Boolean = false
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
            Text("Appearance", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Dynamic color (Material You)") },
                    supportingContent = { Text("Match system wallpaper colors on Android 12+") },
                    leadingContent = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    trailingContent = {
                        Switch(
                            checked = state.useDynamicColor,
                            onCheckedChange = onToggleDynamicColor
                        )
                    }
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
                    }
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
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("Extraction", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Prefer unpac (partition picker)") },
                    supportingContent = { Text("Default engine for single-file flow") },
                    trailingContent = {
                        Switch(
                            checked = state.preferUnpacEngine,
                            onCheckedChange = onPreferUnpacChange
                        )
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("About", style = MaterialTheme.typography.titleMedium)

            Card(modifier = Modifier.fillMaxWidth()) {
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
                    leadingContent = { Icon(Icons.Filled.SystemUpdate, contentDescription = null) }
                )
            }

            if (state.updateAvailable) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onUpdateClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Update")
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
