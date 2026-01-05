package org.skitrace.skitrace.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.maplibre.android.MapLibre
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import org.skitrace.skitrace.map.MapStyles

@Composable
fun MapScreen() {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    SideEffect {
        MapLibre.getInstance(context)
    }

    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = 58.86441106582072, latitude = 57.58123541662825),
            zoom = 12.0
        )
    )

    var isMapLoaded by remember { mutableStateOf(false) }

    val style = remember(isDark) {
        BaseStyle.Json(MapStyles.getDynamicStyle(isDark))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = style,
            cameraState = cameraState,
            options = MapOptions(
                ornamentOptions = OrnamentOptions(
                    isCompassEnabled = true,
                    isLogoEnabled = false,
                    isAttributionEnabled = true
                ),
                gestureOptions = GestureOptions(
                    isTiltEnabled = true
                )
            ),
            onMapLoadFinished = {
                isMapLoaded = true
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallFloatingActionButton(
                onClick = {
                    val currentPos = cameraState.position
                    cameraState.position = currentPos.copy(zoom = currentPos.zoom + 1)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
            }

            Spacer(modifier = Modifier.height(16.dp))

            SmallFloatingActionButton(
                onClick = {
                    val currentPos = cameraState.position
                    cameraState.position = currentPos.copy(zoom = currentPos.zoom - 1)
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        if (!isMapLoaded) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}