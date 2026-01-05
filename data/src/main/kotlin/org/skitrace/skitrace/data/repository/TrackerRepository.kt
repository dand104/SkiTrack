package org.skitrace.skitrace.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.skitrace.skitrace.core.TrackProcessor
import org.skitrace.skitrace.core.model.SkiStatistics
import org.skitrace.skitrace.core.model.TrackPoint
import org.skitrace.skitrace.data.location.LocationClient
import org.skitrace.skitrace.data.sensor.SensorClient
import org.skitrace.skitrace.data.util.DefaultDispatcherProvider
import org.skitrace.skitrace.data.util.DispatcherProvider

private data class RawSensorEvent(
    val type: Int,
    val v0: Float,
    val v1: Float,
    val v2: Float,
    val v3: Float,
    val timestamp: Long
)

class TrackerRepository(
    context: Context,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {

    private val nativeMutex = Mutex()

    private val trackProcessor = TrackProcessor()
    private val locationClient = LocationClient(context)
    private val sensorClient = SensorClient(context)

    private val _currentStats = MutableStateFlow(SkiStatistics())
    val currentStats: StateFlow<SkiStatistics> = _currentStats.asStateFlow()

    private val _trackPoints = MutableSharedFlow<TrackPoint>(replay = 1)
    val trackPoints: SharedFlow<TrackPoint> = _trackPoints.asSharedFlow()

    private var trackingJob: Job? = null

    private val sensorChannel = Channel<RawSensorEvent>(Channel.UNLIMITED)

    fun startTracking(scope: CoroutineScope) {
        if (trackingJob?.isActive == true) return

        trackingJob = scope.launch(dispatchers.default) {
            nativeMutex.withLock {
                trackProcessor.reset()
            }

            val sensorProcessingJob = launch {
                sensorChannel.consumeEach { event ->
                    nativeMutex.withLock {
                        trackProcessor.updateSensors(
                            event.type,
                            event.v0, event.v1, event.v2, event.v3,
                            event.timestamp
                        )
                    }
                }
            }

            sensorClient.startListening { type, v0, v1, v2, v3, timestamp ->
                sensorChannel.trySend(RawSensorEvent(type, v0, v1, v2, v3, timestamp))
            }

            // 3. Обработка локации
            locationClient.getLocationUpdates(2000L)
                .flowOn(dispatchers.io)
                .collect { location ->
                    val now = System.currentTimeMillis()

                    val (point, stats) = nativeMutex.withLock {
                        val p = trackProcessor.processPoint(
                            location.latitude,
                            location.longitude,
                            location.altitude,
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
        while(sensorChannel.tryReceive().isSuccess) { /* drain */ }
    }
}