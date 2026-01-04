package org.skitrace.skitrace.core.model;
import org.jetbrains.annotations.NotNull;

public record TrackPoint(double latitude, double longitude, double altitude, long timestamp) {
    @NotNull
    @Override
    public String toString() {
        return "TrackPoint{lat=" + latitude + ", lon=" + longitude + ", alt=" + altitude + "}";
    }
}