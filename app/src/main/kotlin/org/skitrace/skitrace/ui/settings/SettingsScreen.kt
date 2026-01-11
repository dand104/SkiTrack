package org.skitrace.skitrace.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
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
            modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())
        ) {
            SettingsSection("General") {
                ListItem(headlineContent = { Text("Units") }, supportingContent = { Text("Metric (km, m, km/h)") })
                ListItem(headlineContent = { Text("Theme") }, supportingContent = { Text("System Default") })
            }
            HorizontalDivider()
            SettingsSection("Recording") {
                ListItem(headlineContent = { Text("Auto-pause") }, supportingContent = { Text("Pause recording when stationary") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
                ListItem(headlineContent = { Text("Activity Detection") }, supportingContent = { Text("Detect lifts and ski runs automatically") }, trailingContent = { Switch(checked = true, onCheckedChange = {}) })
            }
            HorizontalDivider()
            SettingsSection("About") {
                ListItem(headlineContent = { Text("SkiTrace") }, supportingContent = { Text("Version 0.1.0-alpha") })
                Text("Open Source Software", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.primary)
                Text(
                    text = """
                        This application uses the following open source libraries:
                        
                        • Maplibre Native (BSD 2 Clause)
                        • maplibre-compose (BSD 3 Clause)
                        • Jetpack Compose (Apache 2.0)
                        • Kotlin Libraries (Apache 2.0)
                        • Accompanist (Apache 2.0)
                        • AndroidX Libraries (Apache 2.0)
                        
                        Map Data © OpenStreetMap contributors, OpenSnowMap.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        content()
    }
}
