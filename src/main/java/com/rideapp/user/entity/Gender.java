package com.rideapp.user.entity;

public enum Gender {
    MALE,
    FEMALE,
    NON_BINARY,
    PREFER_NOT_TO_SAY;

    public boolean isStrictlyCompatibleWith(Gender other) {
        if (other == null) {
            return false;
        }
        if (this == MALE && other == MALE) {
            return true;
        }
        if (this == FEMALE && other == FEMALE) {
            return true;
        }
        return false;
    }
}
