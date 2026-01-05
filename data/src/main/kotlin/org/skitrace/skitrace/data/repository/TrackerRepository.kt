package org.skitrace.skitrace.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.skitrace.skitrace.core.TrackProcessor
import org.skitrace.skitrace.core.model.SkiStatistics
import org.skitrace.skitrace.core.model.TrackPoint
import org.skitrace.skitrace.data.sensor.SensorClient
import org.skitrace.skitrace.data.util.DefaultDispatcherProvider
import org.skitrace.skitrace.data.util.DispatcherProvider
import org.skitrace.skitrace.data.recognition.ActivityClient
import org.skitrace.skitrace.data.di.ServicesProvider
class TrackerRepository(
    context: Context,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {

    private val nativeMutex = Mutex()
    private val trackProcessor = TrackProcessor()
    private val locationClient = ServicesProvider.provideLocationClient(context)
    private val activityClient = ServicesProvider.provideActivityClient(context)
    private val sensorClient = SensorClient(context)

    private val _currentStats = MutableStateFlow(SkiStatistics())
    val currentStats: StateFlow<SkiStatistics> = _currentStats.asStateFlow()

    private val _trackPoints = MutableSharedFlow<TrackPoint>(replay = 1)
    val trackPoints: SharedFlow<TrackPoint> = _trackPoints.asSharedFlow()
    private var trackingJob: Job? = null

    fun startTracking(scope: CoroutineScope) {
        if (trackingJob?.isActive == true) return

        trackingJob = scope.launch(dispatchers.default) {
            nativeMutex.withLock { trackProcessor.reset() }

            sensorClient.startListening { types, v0s, v1s, v2s, v3s, timestamps, count ->
                scope.launch(dispatchers.default) {
                    nativeMutex.withLock {
                        trackProcessor.updateSensorsBatch(types, v0s, v1s, v2s, v3s, timestamps, count)
                    }
                }
            }
            launch {
                activityClient.getActivityUpdates(5000L).collect { activity ->
                    nativeMutex.withLock {
                        trackProcessor.updateActivity(activity.type, activity.confidence)
                    }
                }
            }
            locationClient.getLocationUpdates(1000L)
                .flowOn(dispatchers.io)
                .collect { location ->
                    val now = System.currentTimeMillis()
                    val (point, stats) = nativeMutex.withLock {
                        val p = trackProcessor.processPoint(
                            location.latitude,
                            location.longitude,
                            location.altitude,
                            location.accuracy.toDouble(),
                            now
                        )
                        val s = trackProcessor.getStatistics()
                        p to s
                    }

                    _trackPoints.emit(point)
                    _currentStats.emit(stats)
                }
        }
    }

    fun stopTracking() {
        sensorClient.stopListening()
        trackingJob?.cancel()
        trackingJob = null
    }
}