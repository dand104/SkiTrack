package org.skitrace.skitrace.core;

import org.skitrace.skitrace.core.model.SkiStatistics;
import org.skitrace.skitrace.core.model.TrackPoint;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

public class TrackProcessor implements AutoCloseable {

    static {
        System.loadLibrary("skitrace-core-native");
    }

    private long nativePtr;

    // Point: lat, lon, alt (3 doubles = 24 bytes)
    private final DoubleBuffer pointOutputBuffer;

    // Stats: dist, maxSpd, avgSpd, vDrop, vAscent, curAlt, curSpd, dur (8 doubles = 64 bytes)
    private final DoubleBuffer statsOutputBuffer;

    public TrackProcessor() {
        nativePtr = createNativeProcessor();

        pointOutputBuffer = ByteBuffer.allocateDirect(3 * 8)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();

        statsOutputBuffer = ByteBuffer.allocateDirect(8 * 8)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();
    }

    public TrackPoint processPoint(double lat, double lon, double alt, long timestamp) {
        if (nativePtr == 0) throw new IllegalStateException("Process is closed");

        addPointNative(nativePtr, lat, lon, alt, timestamp, pointOutputBuffer);

        return new TrackPoint(
                pointOutputBuffer.get(0),
                pointOutputBuffer.get(1),
                pointOutputBuffer.get(2),
                timestamp
        );
    }

    public SkiStatistics getStatistics() {
        if (nativePtr == 0) return new SkiStatistics();

        getStatisticsNative(nativePtr, statsOutputBuffer);

        return new SkiStatistics(
                statsOutputBuffer.get(0), // distance
                statsOutputBuffer.get(1), // maxSpeed
                statsOutputBuffer.get(2), // avgSpeed
                statsOutputBuffer.get(3), // verticalDrop
                statsOutputBuffer.get(4), // verticalAscent
                statsOutputBuffer.get(5), // currentAltitude
                statsOutputBuffer.get(6), // currentSpeed
                (long) statsOutputBuffer.get(7) // duration
        );
    }

    public void reset() {
        if (nativePtr != 0) {
            resetNative(nativePtr);
        }
    }

    @Override
    public void close() {
        if (nativePtr != 0) {
            destroyNativeProcessor(nativePtr);
            nativePtr = 0;
        }
    }

    private native long createNativeProcessor();
    private native void destroyNativeProcessor(long ptr);
    private native void resetNative(long ptr);

    private native void addPointNative(long ptr, double lat, double lon, double alt, long timestamp, DoubleBuffer outputBuf);
    private native void getStatisticsNative(long ptr, DoubleBuffer outputBuf);
}