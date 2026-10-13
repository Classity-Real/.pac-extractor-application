package dev.classityreal.pext.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.classityreal.pext.ui.theme.ExpressiveGradientEndDark
import dev.classityreal.pext.ui.theme.ExpressiveGradientEndLight
import dev.classityreal.pext.ui.theme.ExpressiveGradientStartDark
import dev.classityreal.pext.ui.theme.ExpressiveGradientStartLight

/**
 * The app's entry screen. Two ways in:
 *  - [onPickFile]: pick one .pac, inspect it, choose individual partitions to extract.
 *  - [onPickFromList]: pick several .pac files at once for batch extraction — each one gets
 *    fully extracted (no per-partition picking) into its own numbered folder.
 * Material Expressive gradient is drawn behind the content when [useExpressiveBackground] is true.
 */
@Composable
fun HomeScreen(
    onPickFile: () -> Unit,
    onPickFromList: () -> Unit,
    onOpenSettings: () -> Unit = {},
    useExpressiveBackground: Boolean = true
) {
    val dark = isSystemInDarkTheme()
    val gradient = if (dark) {
        Brush.verticalGradient(
            listOf(ExpressiveGradientStartDark, ExpressiveGradientEndDark)
        )
    } else {
        Brush.verticalGradient(
            listOf(ExpressiveGradientStartLight, ExpressiveGradientEndLight)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (useExpressiveBackground) Modifier.background(gradient)
                else Modifier
            )
    ) {
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings")
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Archive,
                contentDescription = null,
                modifier = Modifier.height(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Extract Unisoc .pac firmware",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Pick one .pac file to inspect it and choose individual partitions, " +
                    "or select several at once to extract them all in a batch.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onPickFile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select a file")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onPickFromList,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select from list")
            }
        }
    }
}
