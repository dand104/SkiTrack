package org.skitrace.skitrace.ui.details

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.*
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.LineString
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.toJson
import org.skitrace.skitrace.SkiTraceApplication
import org.skitrace.skitrace.core.model.TrackState
import org.skitrace.skitrace.data.db.entity.TrackPointEntity
import org.skitrace.skitrace.data.db.entity.TrackRunEntity
import org.skitrace.skitrace.data.repository.TrackerRepository
import org.skitrace.skitrace.map.MapStyles

class TrackDetailsViewModel(
    private val repository: TrackerRepository,
    private val runId: Long
) : ViewModel() {
    private val _run = MutableStateFlow<TrackRunEntity?>(null)
    val run = _run.asStateFlow()

    private val _points = MutableStateFlow<List<TrackPointEntity>>(emptyList())
    val points = _points.asStateFlow()

    private val _geoJsonString = MutableStateFlow<String?>(null)
    val geoJsonString = _geoJsonString.asStateFlow()

    init {
        viewModelScope.launch {
            val r = repository.getRun(runId)
            _run.value = r
            val p = repository.getRunPoints(runId)
            _points.value = p

            val features = mutableListOf<Feature<Geometry, JsonObject>>()
            if (p.isNotEmpty()) {
                var segmentPoints = mutableListOf<Position>()
                var lastState = p.first().stateCode

                p.forEach { point ->
                    val pos = Position(point.longitude, point.latitude)

                    if (point.stateCode != lastState && segmentPoints.isNotEmpty()) {
                        segmentPoints.add(pos)

                        if (segmentPoints.size >= 2) {
                            features.add(
                                Feature(
                                    geometry = LineString(segmentPoints.toList()),
                                    properties = buildJsonObject { put("state", lastState) },
                                    id = null
                                )
                            )
                        }

                        segmentPoints = mutableListOf(pos)
                        lastState = point.stateCode
                    } else {
                        segmentPoints.add(pos)
                    }
                }

                if (segmentPoints.size >= 2) {
                    val feature = Feature(
                        geometry = LineString(segmentPoints.toList()),
                        properties = buildJsonObject {
                            put("state", lastState)
                        },
                        id = null
                    )
                    features.add(feature)
                }
            }
            val collection = FeatureCollection(features)
            _geoJsonString.value = collection.toJson()
        }
    }

    fun deleteRun() = viewModelScope.launch { repository.deleteRun(runId) }
    fun renameRun(newName: String) = viewModelScope.launch { repository.updateRunTitle(runId, newName) }
    suspend fun exportRun() = repository.exportRun(runId)

    class Factory(private val repo: TrackerRepository, private val id: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TrackDetailsViewModel(repo, id) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackDetailsScreen(runId: Long, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as SkiTraceApplication
    val viewModel: TrackDetailsViewModel = viewModel(factory = TrackDetailsViewModel.Factory(app.trackerRepository, runId))

    val run by viewModel.run.collectAsState()
    val points by viewModel.points.collectAsState()
    val geoJsonString by viewModel.geoJsonString.collectAsState()

    val bottomSheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )

    if (run == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetPeekHeight = 320.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            TrackDetailsContent(
                run = run!!,
                points = points,
                onRename = { viewModel.renameRun(it) },
                onDelete = { viewModel.deleteRun(); onBack() },
                onExport = { /* Export logic */ }
            )
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            val cameraState = rememberCameraState()

            LaunchedEffect(points) {
                if (points.isNotEmpty()) {
                    val lat = points.map { it.latitude }.average()
                    val lon = points.map { it.longitude }.average()
                    cameraState.position = CameraPosition(
                        target = Position(lon, lat),
                        zoom = 13.0
                    )
                }
            }

            MaplibreMap(
                modifier = Modifier.fillMaxSize().padding(bottom = 300.dp),
                baseStyle = org.maplibre.compose.style.BaseStyle.Uri(MapStyles.STYLE_LIBERTY),
                cameraState = cameraState
            ) {
                if (geoJsonString != null) {
                    val source = rememberGeoJsonSource(
                        data = GeoJsonData.JsonString(geoJsonString!!)
                    )

                    LineLayer(
                        id = "run_layer",
                        source = source,
                        width = const(4.0.dp),
                        color = switch(
                            input = feature["state"].asNumber(),
                            case(TrackState.SKIING.value, const(Color.Blue)),
                            case(TrackState.LIFT.value, const(Color.Red)),
                            fallback = const(Color.Gray)
                        )
                    )
                }
            }

            SmallFloatingActionButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
        }
    }
}

@Composable
fun TrackDetailsContent(
    run: TrackRunEntity,
    points: List<TrackPointEntity>,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onExport) { Icon(Icons.Default.Share, "Export GPX") }
            IconButton(onClick = { /* TODO Dialog */ }) { Icon(Icons.Default.Edit, "Rename") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
        }

        Text(run.note ?: "Ski Session", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Total Distance: %.1f km".format(run.totalDistance / 1000.0), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)

        Spacer(Modifier.height(24.dp))

        if (points.isNotEmpty()) {
            Text("Altitude Profile", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            TrackGraph(points, GraphType.ALTITUDE)
            Spacer(Modifier.height(24.dp))
            Text("Speed Profile", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            TrackGraph(points, GraphType.SPEED)
        }
        Spacer(Modifier.height(48.dp))
    }
}

enum class GraphType { ALTITUDE, SPEED }

@Composable
fun TrackGraph(points: List<TrackPointEntity>, type: GraphType) {
    val skiColor = MaterialTheme.colorScheme.primary
    val liftColor = MaterialTheme.colorScheme.error
    val idleColor = MaterialTheme.colorScheme.outline

    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))) {
        if (points.isEmpty()) return@Canvas
        val width = size.width
        val height = size.height
        val padding = 16.dp.toPx()

        val values = if (type == GraphType.ALTITUDE) points.map { it.altitude } else points.map { it.speedMs * 3.6 }
        val minVal = values.minOrNull() ?: 0.0
        val maxVal = values.maxOrNull() ?: 100.0
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        for (i in 0 until points.size - 1) {
            val x1 = (i.toFloat() / (points.size - 1)) * (width - 2 * padding) + padding
            val y1 = height - padding - (((values[i] - minVal) / range) * (height - 2 * padding)).toFloat()
            val x2 = ((i+1).toFloat() / (points.size - 1)) * (width - 2 * padding) + padding
            val y2 = height - padding - (((values[i+1] - minVal) / range) * (height - 2 * padding)).toFloat()

            val color = when(points[i].stateCode) {
                TrackState.SKIING.value -> skiColor
                TrackState.LIFT.value -> liftColor
                else -> idleColor
            }
            drawLine(
                color = color,
                start = Offset(x1, y1),
                end = Offset(x2, y2),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}
