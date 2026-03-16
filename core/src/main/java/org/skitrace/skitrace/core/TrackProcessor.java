package org.skitrace.skitrace.core;

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
    private final DoubleBuffer outputBuffer;
    public record InstantData(TrackPoint point, double speedMs, TrackState state) {}


    public TrackProcessor() {
        processorPntr = createProcessor();
        outputBuffer = ByteBuffer.allocateDirect(5 * 8).order(ByteOrder.nativeOrder()).asDoubleBuffer();
    }

    public InstantData processPoint(double lat, double lon, double alt, double accuracy, long timestamp) {
        if (processorPntr == 0) throw new IllegalStateException("Process is closed");

        addPoint(processorPntr, lat, lon, alt, accuracy, timestamp, outputBuffer);

        TrackPoint p = new TrackPoint(outputBuffer.get(0), outputBuffer.get(1), outputBuffer.get(2), timestamp);
        double speed = outputBuffer.get(3);
        TrackState state = TrackState.fromInt((int) outputBuffer.get(4));

        return new InstantData(p, speed, state);
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

    private native void updateSensors(long ptr, int[] types, float[] v0, float[] v1, float[] v2, float[] v3, long[] timestamps, int count);
    private native void updateActivity(long ptr, int type, int confidence);

}