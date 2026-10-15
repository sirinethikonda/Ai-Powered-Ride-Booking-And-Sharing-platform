package com.rideapp.payment.entity;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    RECONCILIATION_REQUIRED,
    REFUNDED
}
