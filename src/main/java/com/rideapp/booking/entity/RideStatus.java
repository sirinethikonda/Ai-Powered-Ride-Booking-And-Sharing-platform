package com.rideapp.booking.entity;

public enum RideStatus {
    REQUESTED,
    SEARCHING_FOR_DRIVER,
    DRIVER_ASSIGNED,
    DRIVER_ARRIVING,
    DRIVER_ARRIVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED || this == FAILED;
    }

    public boolean canTransitionTo(RideStatus target) {
        if (this.isTerminal()) {
            return false;
        }
        return switch (this) {
            case REQUESTED -> target == SEARCHING_FOR_DRIVER || target == CANCELLED;
            case SEARCHING_FOR_DRIVER -> target == DRIVER_ASSIGNED || target == CANCELLED || target == FAILED;
            case DRIVER_ASSIGNED -> target == DRIVER_ARRIVING || target == SEARCHING_FOR_DRIVER || target == CANCELLED;
            case DRIVER_ARRIVING -> target == DRIVER_ARRIVED || target == SEARCHING_FOR_DRIVER || target == CANCELLED;
            case DRIVER_ARRIVED -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == COMPLETED;
            default -> false;
        };
    }
}
