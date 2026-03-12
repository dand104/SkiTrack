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

    private long processorPntr;
    private final DoubleBuffer pointOutputBuffer;
    private final DoubleBuffer statsOutputBuffer;

    public TrackProcessor() {
        processorPntr = createProcessor();
        pointOutputBuffer = ByteBuffer.allocateDirect(3 * 8)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();

        statsOutputBuffer = ByteBuffer.allocateDirect(11 * 8)
                .order(ByteOrder.nativeOrder())
                .asDoubleBuffer();
    }

    public TrackPoint processPoint(double lat, double lon, double alt, double accuracy, long timestamp) {
        if (processorPntr == 0) throw new IllegalStateException("Process is closed");

        addPoint(processorPntr, lat, lon, alt, accuracy, timestamp, pointOutputBuffer);
        return new TrackPoint(pointOutputBuffer.get(0), pointOutputBuffer.get(1), pointOutputBuffer.get(2), timestamp);
    }

    public SkiStatistics getStatistics() {
        if (processorPntr == 0) return new SkiStatistics();
        fetchTrackData(processorPntr, statsOutputBuffer);

        return new SkiStatistics(
                statsOutputBuffer.get(0), statsOutputBuffer.get(1), statsOutputBuffer.get(2),
                statsOutputBuffer.get(3), statsOutputBuffer.get(4), statsOutputBuffer.get(5),
                statsOutputBuffer.get(6), (long) statsOutputBuffer.get(7), 
                (long) statsOutputBuffer.get(9), (long) statsOutputBuffer.get(10),
                TrackState.fromInt((int) statsOutputBuffer.get(8))
        );
    }

    public void reset() {
        if (processorPntr != 0) resetProcessor(processorPntr);
    }

    @Override
    public void close() {
        if (processorPntr != 0) {
            destroyProcessor(processorPntr);
            processorPntr = 0;
        }
    }

    public void updateSensorsBatch(int[] types, float[] v0s, float[] v1s, float[] v2s, float[] v3s, long[] timestamps, int count) {
        if (processorPntr != 0) {
            updateSensors(processorPntr, types, v0s, v1s, v2s, v3s, timestamps, count);
        }
    }

    public void updateActivity(int type, int confidence) {
        if (processorPntr != 0) {
            updateActivity(processorPntr, type, confidence);
        }
    }

    private native long createProcessor();
    private native void destroyProcessor(long ptr);
    private native void resetProcessor(long ptr);

    private native void addPoint(long ptr, double lat, double lon, double alt, double accuracy, long timestamp, DoubleBuffer outputBuf);
    private native void fetchTrackData(long ptr, DoubleBuffer outputBuf);

    private native void updateSensors(long ptr, int[] types, float[] v0, float[] v1, float[] v2, float[] v3, long[] timestamps, int count);
    private native void updateActivity(long ptr, int type, int confidence);

}