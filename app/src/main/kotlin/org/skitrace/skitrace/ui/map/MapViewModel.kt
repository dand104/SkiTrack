package org.skitrace.skitrace.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.spatialk.geojson.Position
import org.skitrace.skitrace.core.model.TrackPoint
import org.skitrace.skitrace.data.repository.TrackerRepository

class MapViewModel(private val repository: TrackerRepository) : ViewModel() {
    val cameraState = CameraState(
        firstPosition = CameraPosition(
            target = Position(longitude = 58.86441106582072, latitude = 57.58123541662825),
            zoom = 12.0
        )
    )

    private val _currentPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
    val currentPoints = _currentPoints.asStateFlow()

    private val _lastLocation = MutableStateFlow<TrackPoint?>(null)
    val lastLocation = _lastLocation.asStateFlow()

    private var trackingJob: Job? = null

    fun setActive(isActive: Boolean) {
        if (isActive && trackingJob == null) {
            trackingJob = viewModelScope.launch {
                repository.trackPoints.collect { point ->
                    _lastLocation.value = point
                    if (_currentPoints.value.lastOrNull()?.timestamp != point.timestamp) {
                        _currentPoints.value += point
                    }
                }
            }
        } else if (!isActive) {
            trackingJob?.cancel()
            trackingJob = null
        }
    }

    fun zoomToLocation(scope: CoroutineScope) {
        lastLocation.value?.let { loc ->
            scope.launch {
                cameraState.animateTo(
                    CameraPosition(
                        target = Position(longitude = loc.longitude, latitude = loc.latitude),
                        zoom = 15.0
                    )
                )
            }
        }
    }

    class Factory(private val repo: TrackerRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MapViewModel(repo) as T
    }
}
