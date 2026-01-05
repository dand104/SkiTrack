package org.skitrace.skitrace.core.model;

public enum TrackState {
    IDLE(0),
    SKIING(1),
    LIFT(2);

    private final int value;

    TrackState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static TrackState fromInt(int value) {
        for (TrackState state : values()) {
            if (state.value == value) return state;
        }
        return IDLE;
    }
}