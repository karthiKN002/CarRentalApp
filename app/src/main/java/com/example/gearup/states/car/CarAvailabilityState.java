package com.example.gearup.states.car;

public enum CarAvailabilityState {
    PENDING,
    AVAILABLE,
    RENTED,
    MAINTENANCE,
    UNAVAILABLE;

    @Override
    public String toString() {
        return name();
    }
}