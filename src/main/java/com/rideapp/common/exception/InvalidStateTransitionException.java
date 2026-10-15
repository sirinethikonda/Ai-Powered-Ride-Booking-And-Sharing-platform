package com.rideapp.common.exception;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(String currentState, String attemptedAction) {
        super(String.format("Cannot perform action '%s' when current state is '%s'", attemptedAction, currentState));
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
