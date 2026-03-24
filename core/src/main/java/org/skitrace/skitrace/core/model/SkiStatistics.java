package org.skitrace.skitrace.core.model;

public record SkiStatistics(double totalDistanceMeters, double maxSpeedMs, double avgSpeedMs, double verticalDropMeters,
                            double verticalAscentMeters, double currentAltitude, double currentSpeedMs,
                            long totalDurationMs, long skiingDurationMs, long liftDurationMs, int descentsCount, TrackState state) {

    public SkiStatistics() {
        this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, TrackState.IDLE);
    }
}