package org.skitrace.skitrace.core;

import org.skitrace.skitrace.core.model.SkiStatistics;
import org.skitrace.skitrace.core.model.TrackPoint;
import org.skitrace.skitrace.core.model.TrackState;

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

    private final DoubleBuffer statsOutputBuffer;

    public TrackProcessor() {
        nativePtr = createNativeProcessor();

        pointOutputBuffer = ByteBuffer.allocateDirect(3 * 8)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();

        statsOutputBuffer = ByteBuffer.allocateDirect(11 * 8)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();
    }

    public TrackPoint processPoint(double lat, double lon, double alt, double accuracy, long timestamp) {
        if (nativePtr == 0) throw new IllegalStateException("Process is closed");

        addPointNative(nativePtr, lat, lon, alt, accuracy, timestamp, pointOutputBuffer);
        return new TrackPoint(pointOutputBuffer.get(0), pointOutputBuffer.get(1), pointOutputBuffer.get(2), timestamp);
    }

    public SkiStatistics getStatistics() {
        if (nativePtr == 0) return new SkiStatistics();
        getStatisticsNative(nativePtr, statsOutputBuffer);

        return new SkiStatistics(
                statsOutputBuffer.get(0), statsOutputBuffer.get(1), statsOutputBuffer.get(2),
                statsOutputBuffer.get(3), statsOutputBuffer.get(4), statsOutputBuffer.get(5),
                statsOutputBuffer.get(6), (long) statsOutputBuffer.get(7), 
                (long) statsOutputBuffer.get(9), (long) statsOutputBuffer.get(10),
                TrackState.fromInt((int) statsOutputBuffer.get(8))
        );
    }

    public void reset() {
        if (nativePtr != 0) resetNative(nativePtr);
    }

    @Override
    public void close() {
        if (nativePtr != 0) {
            destroyNativeProcessor(nativePtr);
            nativePtr = 0;
        }
    }
    public void updateSensorsBatch(int[] types, float[] v0s, float[] v1s, float[] v2s, float[] v3s, long[] timestamps, int count) {
        if (nativePtr != 0) {
            updateSensorsBatchNative(nativePtr, types, v0s, v1s, v2s, v3s, timestamps, count);
        }
    }
    public void updateActivity(int type, int confidence) {
        if (nativePtr != 0) {
            updateActivityNative(nativePtr, type, confidence);
        }
    }
    private native long createNativeProcessor();
    private native void destroyNativeProcessor(long ptr);
    private native void resetNative(long ptr);

    private native void addPointNative(long ptr, double lat, double lon, double alt, double accuracy, long timestamp, DoubleBuffer outputBuf);
    private native void getStatisticsNative(long ptr, DoubleBuffer outputBuf);

    private native void updateSensorsBatchNative(long ptr, int[] types, float[] v0, float[] v1, float[] v2, float[] v3, long[] timestamps, int count);
    private native void updateActivityNative(long ptr, int type, int confidence);

}