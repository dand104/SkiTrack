package org.skitrace.skitrace.ui.map

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.skitrace.skitrace.map.MapStyles

@Composable
fun MapScreen(viewModel: MapViewModel = viewModel()) {
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current

    var isMapLoaded by remember { mutableStateOf(false) }
    var showAttributionDialog by remember { mutableStateOf(false) }

    val style = remember(isDark) {
        BaseStyle.Json(MapStyles.getDynamicStyle(isDark))
    }
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowInsetsControllerCompat(window, view)
            controller.isAppearanceLightStatusBars = !isDark
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = style,
            cameraState = viewModel.cameraState,
            options = remember {
                MapOptions(
                    ornamentOptions = OrnamentOptions(
                        isCompassEnabled = true,
                        isLogoEnabled = false,
                        isAttributionEnabled = false
                    ),
                    gestureOptions = GestureOptions(isTiltEnabled = true)
                )
            },
            onMapLoadFinished = {
                isMapLoaded = true
            }
        )

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallFloatingActionButton(
                onClick = {
                    val currentPos = viewModel.cameraState.position
                    viewModel.cameraState.position = currentPos.copy(zoom = currentPos.zoom + 1)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
            }

            Spacer(modifier = Modifier.height(16.dp))

            SmallFloatingActionButton(
                onClick = {
                    val currentPos = viewModel.cameraState.position
                    viewModel.cameraState.position = currentPos.copy(zoom = currentPos.zoom - 1)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        IconButton(
            onClick = { showAttributionDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(8.dp)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Attribution",
                tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.6f)
            )
        }

        if (!isMapLoaded) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (showAttributionDialog) {
            AlertDialog(
                onDismissRequest = { showAttributionDialog = false },
                title = { Text("Map Data Attribution") },
                text = {
                    Column {
                        Text("© OpenStreetMap contributors", style = MaterialTheme.typography.bodyMedium)
                        Text("© CARTO", style = MaterialTheme.typography.bodyMedium)
                        Text("© OpenSnowMap", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Powered by MapLibre Native", style = MaterialTheme.typography.labelSmall)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAttributionDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}