package org.skitrace.skitrace.data.repository

import android.location.Location
import android.os.SystemClock
import org.skitrace.skitrace.core.TrackProcessor
import org.skitrace.skitrace.core.model.SkiStatistics
import org.skitrace.skitrace.core.model.TrackPoint
import org.skitrace.skitrace.core.model.TrackState
import kotlin.math.abs

class TrackerStats {
    private var totalDistanceMeters = 0.0
    private var maxSpeedMs = 0.0
    private var verticalDropMeters = 0.0
    private var verticalAscentMeters = 0.0
    private var totalDurationMs = 0L
    private var skiingDurationMs = 0L
    private var liftDurationMs = 0L
    private var descentsCount = 0

    private var trackingStartTimeNs: Long = 0
    private var lastPoint: TrackPoint? = null
    private var lastPointTimeNs: Long = 0
    private var currentAlt = 0.0
    private var currentSpeed = 0.0
    private var currentState = TrackState.IDLE

    private var totalPausedNs: Long = 0
    private var currentPauseStartNs: Long = 0

    var isPaused: Boolean = false
        private set

    private val distanceResults = FloatArray(1)

    fun reset() {
        totalDistanceMeters = 0.0
        maxSpeedMs = 0.0
        verticalDropMeters = 0.0
        verticalAscentMeters = 0.0
        totalDurationMs = 0L
        skiingDurationMs = 0L
        liftDurationMs = 0L
        descentsCount = 0
        trackingStartTimeNs = SystemClock.elapsedRealtimeNanos()
        lastPointTimeNs = 0
        lastPoint = null
        currentAlt = 0.0
        currentSpeed = 0.0
        currentState = TrackState.IDLE
        totalPausedNs = 0
        currentPauseStartNs = 0
        isPaused = false
    }

    fun pause() {
        if (!isPaused) {
            isPaused = true
            currentPauseStartNs = SystemClock.elapsedRealtimeNanos()
            currentState = TrackState.IDLE
            currentSpeed = 0.0
        }
    }

    fun resume() {
        if (isPaused) {
            isPaused = false
            totalPausedNs += SystemClock.elapsedRealtimeNanos() - currentPauseStartNs
            currentPauseStartNs = 0
        }
    }

    private fun calculateEffectiveDuration(timestampNs: Long): Long {
        if (trackingStartTimeNs == 0L) return 0L
        val activePauseNs = if (isPaused) (timestampNs - currentPauseStartNs) else 0L
        return (timestampNs - trackingStartTimeNs - totalPausedNs - activePauseNs) / 1_000_000L
    }

    fun updateTime(): SkiStatistics {
        if (trackingStartTimeNs > 0) {
            totalDurationMs = calculateEffectiveDuration(SystemClock.elapsedRealtimeNanos())
        }
        return buildStats(currentAlt, currentSpeed, currentState)
    }

    fun update(instantData: TrackProcessor.InstantData, timestampNs: Long): SkiStatistics {
        val currentPoint = instantData.point()
        val newState = instantData.state()
        currentSpeed = instantData.speedMs()
        currentAlt = currentPoint.altitude()
        if (newState == TrackState.SKIING && currentState != TrackState.SKIING) {
            descentsCount++
        }
        currentState = newState

        totalDurationMs = calculateEffectiveDuration(timestampNs)

        if (lastPoint == null) {
            lastPointTimeNs = timestampNs
            lastPoint = currentPoint
            return buildStats(currentAlt, currentSpeed, currentState)
        }

        val dtNs = timestampNs - lastPointTimeNs
        val dtMs = dtNs / 1_000_000L
        lastPointTimeNs = timestampNs

        when (currentState) {
            TrackState.SKIING -> skiingDurationMs += dtMs
            TrackState.LIFT -> liftDurationMs += dtMs
            else -> {}
        }

        if (currentSpeed in 0.1..45.0 && currentSpeed > maxSpeedMs) {
            maxSpeedMs = currentSpeed
        }

        val prevPoint = lastPoint!!
        Location.distanceBetween(
            prevPoint.latitude(), prevPoint.longitude(),
            currentPoint.latitude(), currentPoint.longitude(),
            distanceResults
        )
        val distMeters = distanceResults[0].toDouble()

        if (currentState != TrackState.IDLE) {
            totalDistanceMeters += distMeters
        }

        val altDiff = currentAlt - prevPoint.altitude()
        if (abs(altDiff) > 0.1) {
            if (altDiff < 0) {
                verticalDropMeters += abs(altDiff)
            } else {
                verticalAscentMeters += altDiff
            }
        }

        lastPoint = currentPoint

        return buildStats(currentAlt, currentSpeed, currentState)
    }

    private fun buildStats(currentAlt: Double, currentSpeed: Double, state: TrackState): SkiStatistics {
        val avgSpeed = if (totalDurationMs > 0) totalDistanceMeters / (totalDurationMs / 1000.0) else 0.0

        return SkiStatistics(
            totalDistanceMeters,
            maxSpeedMs,
            avgSpeed,
            verticalDropMeters,
            verticalAscentMeters,
            currentAlt,
            currentSpeed,
            totalDurationMs,
            skiingDurationMs,
            liftDurationMs,
            descentsCount,
            if (isPaused) TrackState.IDLE else state
        )
    }
}