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
import org.skitrace.skitrace.data.export.GpxExporter
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
    private val stats = TrackerStats()
    private val locationClient = ServicesProvider.provideLocationClient(context)
    private val activityClient = ServicesProvider.provideActivityClient(context)
    private val sensorClient = SensorClient(context)
    private val trackDao = database.trackDao()

    private val _currentStats = MutableStateFlow(SkiStatistics())
    private val gpxExporter = GpxExporter(context, database, dispatchers)
    val currentStats: StateFlow<SkiStatistics> = _currentStats.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val _trackPoints = MutableSharedFlow<TrackPoint>(replay = 0)
    val trackPoints: SharedFlow<TrackPoint> = _trackPoints.asSharedFlow()

    private var trackingJob: Job? = null
    private var currentRunId: Long? = null

    val allRuns = trackDao.getAllRuns()
    val totalLifetimeDistance = trackDao.getTotalDistance().map { it ?: 0.0 }
    val maxLifetimeSpeed = trackDao.getMaxSpeed().map { it ?: 0.0 }
    val totalLifetimeVertical = trackDao.getTotalVerticalDrop().map { it ?: 0.0 }

    suspend fun getRun(id: Long) = trackDao.getRunById(id)
    suspend fun getRunPoints(id: Long) = trackDao.getPointsForRun(id)
    suspend fun deleteRun(id: Long) = trackDao.deleteRunById(id)
    suspend fun updateRunTitle(id: Long, title: String) {
        val run = trackDao.getRunById(id)
        if (run != null) trackDao.updateRun(run.copy(note = title))
    }

    suspend fun exportRun(runId: Long): android.net.Uri? {
        return gpxExporter.exportRunToGpx(runId)
    }

    fun startTracking(scope: CoroutineScope) {
        if (_isTracking.value) return

        trackingJob = scope.launch(dispatchers.default) {
            nativeMutex.withLock {
                trackProcessor.reset()
                stats.reset()
            }

            val newRun = TrackRunEntity(startTime = System.currentTimeMillis())
            currentRunId = trackDao.insertRun(newRun)
            _isTracking.emit(true)

            launch {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    val updatedStats = nativeMutex.withLock {
                        stats.updateTime()
                    }
                    _currentStats.emit(updatedStats)
                }
            }

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
                    val (point, stat) = nativeMutex.withLock {
                        val timestampNs = location.elapsedRealtimeNanos

                        val instantData = trackProcessor.processPoint(
                            location.latitude,
                            location.longitude,
                            location.altitude,
                            location.accuracy.toDouble(),
                            timestampNs
                        )

                        val s = stats.update(instantData, timestampNs)

                        instantData.point() to s
                    }

                    _trackPoints.emit(point)
                    _currentStats.emit(stat)
                    currentRunId?.let { runId ->
                        trackDao.insertPoint(
                            TrackPointEntity(
                                runId = runId,
                                latitude = point.latitude(),
                                longitude = point.longitude(),
                                altitude = point.altitude(),
                                timestamp = System.currentTimeMillis(),
                                speedMs = stat.currentSpeedMs(),
                                accuracy = location.accuracy.toDouble(),
                                stateCode = stat.state().value
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
                    startTime = System.currentTimeMillis() - finalStats.totalDurationMs(),
                    endTime = System.currentTimeMillis(),
                    totalDistance = finalStats.totalDistanceMeters(),
                    maxSpeed = finalStats.maxSpeedMs(),
                    avgSpeed = finalStats.avgSpeedMs(),
                    verticalDrop = finalStats.verticalDropMeters(),
                    durationMs = finalStats.totalDurationMs(),
                    activeSkiingMs = finalStats.skiingDurationMs(),
                    liftMs = finalStats.liftDurationMs()
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