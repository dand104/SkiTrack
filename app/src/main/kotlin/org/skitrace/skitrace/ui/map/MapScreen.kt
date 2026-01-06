package org.skitrace.skitrace.ui.map

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.launch
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.material3.DisappearingScaleBar
import org.maplibre.compose.material3.ExpandingAttributionButton
import org.maplibre.compose.sources.TileSetOptions
import org.maplibre.compose.sources.rememberRasterSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.skitrace.skitrace.map.MapStyles

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val isDark = isSystemInDarkTheme()
    val view = LocalView.current
    var isMapLoaded by remember { mutableStateOf(false) }
    val styleState = rememberStyleState()
    val scope = rememberCoroutineScope()
    val styleUrl = remember(isDark) {
        if (isDark) MapStyles.STYLE_DARK else MapStyles.STYLE_LIBERTY
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
            baseStyle = BaseStyle.Uri(styleUrl),
            cameraState = viewModel.cameraState,
            styleState = styleState,
            options = remember {
                MapOptions(
                    ornamentOptions = OrnamentOptions.AllDisabled,
                    gestureOptions = GestureOptions(isTiltEnabled = true),
                    renderOptions = org.maplibre.compose.map.RenderOptions(
                        foregroundLoadColor = if (isDark) Color(0xFF262626) else Color(0xFFF0F0F0)
                    )
                )
            },
            onMapLoadFinished = { isMapLoaded = true }
        ) {

            val pistesSource = rememberRasterSource(
                tiles = listOf(MapStyles.TILE_PISTES_URL),
                tileSize = 256,
                options = TileSetOptions(
                    attributionHtml = MapStyles.ATTRIBUTION_OPENSNOW
                )
            )

            Anchor.Top {
                RasterLayer(
                    id = "pistes_layer",
                    source = pistesSource,
                    opacity = const(if (isDark) 0.85f else 1.0f)
                )
            }
        }
        DisappearingScaleBar(
            metersPerDp = viewModel.cameraState.metersPerDpAtTarget,
            zoom = viewModel.cameraState.position.zoom,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = contentPadding.calculateTopPadding() + 16.dp, start = 16.dp)
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = contentPadding.calculateTopPadding() + 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp) // Отступ между элементами
        ) {
            DisappearingCompassButton(
                cameraState = viewModel.cameraState,
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            )

            SmallFloatingActionButton(
                onClick = {
                    scope.launch {
                        val currentPos = viewModel.cameraState.position
                        viewModel.cameraState.animateTo(currentPos.copy(zoom = currentPos.zoom - 1))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Zoom In")
            }

            SmallFloatingActionButton(
                onClick = {
                    scope.launch {
                        val currentPos = viewModel.cameraState.position
                        viewModel.cameraState.animateTo(currentPos.copy(zoom = currentPos.zoom + 1))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        ExpandingAttributionButton(
            cameraState = viewModel.cameraState,
            styleState = styleState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = contentPadding.calculateBottomPadding() + 8.dp, end = 8.dp),
        )

        if (!isMapLoaded) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}