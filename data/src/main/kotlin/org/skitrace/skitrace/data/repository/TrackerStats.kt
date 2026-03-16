package org.skitrace.skitrace.data.repository

import android.location.Location
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

    private var firstPointTimeNs: Long = 0
    private var lastPoint: TrackPoint? = null
    private var lastPointTimeNs: Long = 0

    private val distanceResults = FloatArray(1)

    fun reset() {
        totalDistanceMeters = 0.0
        maxSpeedMs = 0.0
        verticalDropMeters = 0.0
        verticalAscentMeters = 0.0
        totalDurationMs = 0L
        skiingDurationMs = 0L
        liftDurationMs = 0L
        firstPointTimeNs = 0
        lastPointTimeNs = 0
        lastPoint = null
    }

    fun update(instantData: TrackProcessor.InstantData, timestampNs: Long): SkiStatistics {
        val currentPoint = instantData.point()
        val currentState = instantData.state()
        val currentSpeed = instantData.speedMs()

        if (lastPoint == null) {
            firstPointTimeNs = timestampNs
            lastPointTimeNs = timestampNs
            lastPoint = currentPoint
            return buildStats(currentPoint.altitude(), currentSpeed, currentState)
        }

        val dtNs = timestampNs - lastPointTimeNs
        val dtMs = dtNs / 1_000_000L
        lastPointTimeNs = timestampNs
        totalDurationMs = (timestampNs - firstPointTimeNs) / 1_000_000L

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

        val altDiff = currentPoint.altitude() - prevPoint.altitude()
        if (abs(altDiff) > 0.1) {
            if (altDiff < 0) {
                verticalDropMeters += abs(altDiff)
            } else {
                verticalAscentMeters += altDiff
            }
        }

        lastPoint = currentPoint

        return buildStats(currentPoint.altitude(), currentSpeed, currentState)
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
            state
        )
    }
}