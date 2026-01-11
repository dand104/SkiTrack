package org.skitrace.skitrace.ui.map

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.Anchor
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.RasterLayer
import org.maplibre.compose.map.GestureOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.material3.DisappearingCompassButton
import org.maplibre.compose.material3.DisappearingScaleBar
import org.maplibre.compose.material3.ExpandingAttributionButton
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.TileSetOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.sources.rememberRasterSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.style.rememberStyleState
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.toJson
import org.skitrace.skitrace.SkiTraceApplication
import org.skitrace.skitrace.map.MapStyles

@Composable
fun MapScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val context = LocalContext.current
    val app = context.applicationContext as SkiTraceApplication
    val viewModel: MapViewModel = viewModel(factory = MapViewModel.Factory(app.trackerRepository))

    val isDark = isSystemInDarkTheme()
    val view = LocalView.current
    var isMapLoaded by remember { mutableStateOf(false) }
    val styleState = rememberStyleState()
    val scope = rememberCoroutineScope()

    val currentPoints by viewModel.currentPoints.collectAsState()
    val lastLocation by viewModel.lastLocation.collectAsState()

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

            val trackGeoJsonString = remember(currentPoints) {
                if (currentPoints.size < 2) {
                    FeatureCollection(emptyList<Feature<Geometry, JsonObject>>()).toJson()
                } else {
                    val line = LineString(
                        currentPoints.map { Position(it.longitude, it.latitude) }
                    )
                    FeatureCollection(
                        listOf(
                            Feature(
                                geometry = line,
                                properties = buildJsonObject { }, // Empty non-null JsonObject
                                id = null
                            )
                        )
                    ).toJson()
                }
            }

            val trackSource = rememberGeoJsonSource(
                data = GeoJsonData.JsonString(trackGeoJsonString)
            )

            LineLayer(
                id = "live_track",
                source = trackSource,
                color = const(Color.Magenta),
                width = const(4.0.dp)
            )

            if (lastLocation != null) {
                val locGeoJsonString = remember(lastLocation) {
                    FeatureCollection(
                        listOf(
                            Feature(
                                geometry = Point(
                                    Position(lastLocation!!.longitude, lastLocation!!.latitude)
                                ),
                                properties = buildJsonObject { },
                                id = null
                            )
                        )
                    ).toJson()
                }

                val locSource = rememberGeoJsonSource(
                    data = GeoJsonData.JsonString(locGeoJsonString)
                )

                CircleLayer(
                    id = "my_location",
                    source = locSource,
                    color = const(Color.Blue),
                    radius = const(8.0.dp),
                    strokeWidth = const(2.0.dp),
                    strokeColor = const(Color.White)
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        viewModel.cameraState.animateTo(currentPos.copy(zoom = currentPos.zoom + 1))
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
                        viewModel.cameraState.animateTo(currentPos.copy(zoom = currentPos.zoom - 1))
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "Zoom Out")
            }

            SmallFloatingActionButton(
                onClick = { viewModel.zoomToLocation(scope) },
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(imageVector = Icons.Default.MyLocation, contentDescription = "Locate")
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
