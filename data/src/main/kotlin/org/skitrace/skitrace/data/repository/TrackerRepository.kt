package org.skitrace.skitrace.data.repository

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.skitrace.skitrace.core.TrackProcessor
import org.skitrace.skitrace.core.model.SkiStatistics
import org.skitrace.skitrace.core.model.TrackPoint
import org.skitrace.skitrace.data.db.SkiDatabase
import org.skitrace.skitrace.data.db.entity.TrackPointEntity
import org.skitrace.skitrace.data.db.entity.TrackRunEntity
import org.skitrace.skitrace.data.di.ServicesProvider
import org.skitrace.skitrace.data.sensor.SensorClient
import org.skitrace.skitrace.data.util.DefaultDispatcherProvider
import org.skitrace.skitrace.data.util.DispatcherProvider

class TrackerRepository(
    context: Context,
    private val database: SkiDatabase = SkiDatabase.getDatabase(context),
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) {

    private val nativeMutex = Mutex()
    private val trackProcessor = TrackProcessor()
    private val locationClient = ServicesProvider.provideLocationClient(context)
    private val activityClient = ServicesProvider.provideActivityClient(context)
    private val sensorClient = SensorClient(context)
    private val trackDao = database.trackDao()

    private val _currentStats = MutableStateFlow(SkiStatistics())
    val currentStats: StateFlow<SkiStatistics> = _currentStats.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _trackPoints = MutableSharedFlow<TrackPoint>(replay = 1)
    val trackPoints: SharedFlow<TrackPoint> = _trackPoints.asSharedFlow()

    private var trackingJob: Job? = null
    private var currentRunId: Long? = null

    val allRuns = trackDao.getAllRuns()
    val totalLifetimeDistance = trackDao.getTotalDistance().map { it ?: 0.0 }
    val maxLifetimeSpeed = trackDao.getMaxSpeed().map { it ?: 0.0 }
    val totalLifetimeVertical = trackDao.getTotalVerticalDrop().map { it ?: 0.0 }

    fun startTracking(scope: CoroutineScope) {
        if (_isTracking.value) return

        trackingJob = scope.launch(dispatchers.default) {
            nativeMutex.withLock { trackProcessor.reset() }

            val newRun = TrackRunEntity(startTime = System.currentTimeMillis())
            currentRunId = trackDao.insertRun(newRun)
            _isTracking.emit(true)

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
                    val (point, stats) = nativeMutex.withLock {
                        val timestampNs = location.elapsedRealtimeNanos
                        val p = trackProcessor.processPoint(
                            location.latitude,
                            location.longitude,
                            location.altitude,
                            location.accuracy.toDouble(),
                            timestampNs
                        )
                        val s = trackProcessor.getStatistics()
                        p to s
                    }
                    _trackPoints.emit(point)
                    _currentStats.emit(stats)

                    currentRunId?.let { runId ->
                        trackDao.insertPoint(
                            TrackPointEntity(
                                runId = runId,
                                latitude = point.latitude(),
                                longitude = point.longitude(),
                                altitude = point.altitude(),
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
        }
    }

    fun stopTracking() {
        val job = trackingJob
        trackingJob = null
        sensorClient.stopListening()

        val runId = currentRunId
        val finalStats = _currentStats.value

        if (runId != null && job != null) {
            CoroutineScope(dispatchers.io).launch {
                job.cancelAndJoin()

                val runEntity = TrackRunEntity(
                    id = runId,
                    startTime = System.currentTimeMillis() - finalStats.durationMs(),
                    endTime = System.currentTimeMillis(),
                    totalDistance = finalStats.totalDistanceMeters(),
                    maxSpeed = finalStats.maxSpeedMs(),
                    avgSpeed = finalStats.avgSpeedMs(),
                    verticalDrop = finalStats.verticalDropMeters(),
                    durationMs = finalStats.durationMs()
                )
                trackDao.updateRun(runEntity)
                currentRunId = null
                _isTracking.emit(false)
                _currentStats.emit(SkiStatistics())
            }
        } else {
            _isTracking.value = false
        }
    }
}