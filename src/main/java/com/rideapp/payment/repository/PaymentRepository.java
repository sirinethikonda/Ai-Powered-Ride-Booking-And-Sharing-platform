package com.rideapp.payment.repository;

import com.rideapp.payment.entity.Payment;
import com.rideapp.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByRideId(UUID rideId);
    List<Payment> findByStatus(PaymentStatus status);
}
