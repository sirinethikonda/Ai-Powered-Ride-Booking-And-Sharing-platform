package com.rideapp.payment.service;

import com.rideapp.booking.entity.Ride;
import com.rideapp.booking.repository.RideRepository;
import com.rideapp.common.exception.BusinessRuleException;
import com.rideapp.common.exception.ResourceNotFoundException;
import com.rideapp.messaging.event.PaymentFailedEvent;
import com.rideapp.messaging.event.PaymentSucceededEvent;
import com.rideapp.messaging.outbox.OutboxService;
import com.rideapp.payment.entity.Payment;
import com.rideapp.payment.entity.PaymentStatus;
import com.rideapp.payment.entity.PaymentTransaction;
import com.rideapp.payment.gateway.MockPaymentGateway;
import com.rideapp.payment.gateway.PaymentGateway;
import com.rideapp.payment.gateway.PaymentGatewayRequest;
import com.rideapp.payment.gateway.PaymentGatewayResponse;
import com.rideapp.payment.repository.PaymentRepository;
import com.rideapp.payment.repository.PaymentTransactionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RideRepository rideRepository;
    private final PaymentGateway paymentGateway;
    private final OutboxService outboxService;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            RideRepository rideRepository,
            PaymentGateway paymentGateway,
            OutboxService outboxService) {
        this.paymentRepository = paymentRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.rideRepository = rideRepository;
        this.paymentGateway = paymentGateway;
        this.outboxService = outboxService;
    }

    @Transactional
    public Payment processRidePayment(UUID rideId, String idempotencyKey) {
        // Idempotency check: if payment already exists with key, return it
        Optional<Payment> existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existingPayment.isPresent()) {
            Payment p = existingPayment.get();
            if (p.getStatus() == PaymentStatus.SUCCEEDED) {
                log.info("Payment with idempotency key {} is already settled as SUCCEEDED", idempotencyKey);
                return p;
            }
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", rideId));

        BigDecimal amount = ride.getFareBreakdown() != null ? ride.getFareBreakdown().getFinalFare() : BigDecimal.valueOf(100);

        Payment payment = existingPayment.orElseGet(() ->
                paymentRepository.save(new Payment(ride, ride.getPassenger(), amount, "INR", idempotencyKey))
        );

        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.save(payment);

        return executePaymentChargeWithResilience(payment, ride);
    }

    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "chargeFallback")
    @Retry(name = "paymentGateway")
    @RateLimiter(name = "paymentGateway")
    public Payment executePaymentChargeWithResilience(Payment payment, Ride ride) {
        PaymentGatewayRequest request = new PaymentGatewayRequest(
                payment.getId(),
                ride.getId(),
                ride.getPassenger().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getIdempotencyKey()
        );

        PaymentGatewayResponse response = paymentGateway.charge(request);

        if (response.successful()) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            PaymentTransaction tx = new PaymentTransaction(
                    payment, response.transactionReference(), "CHARGE", "SUCCESS", null, null
            );
            paymentTransactionRepository.save(tx);
            payment = paymentRepository.save(payment);

            outboxService.recordEvent(
                    "Payment",
                    payment.getId().toString(),
                    "PaymentSucceededEvent",
                    "payment.events",
                    payment.getId().toString(),
                    PaymentSucceededEvent.of(payment.getId(), ride.getId(), payment.getAmount(), response.transactionReference())
            );
        } else if ("PENDING".equals(response.status())) {
            // Uncertain outcome: Retain pending state for reconciliation instead of duplicate charges
            payment.setStatus(PaymentStatus.RECONCILIATION_REQUIRED);
            PaymentTransaction tx = new PaymentTransaction(
                    payment, response.transactionReference(), "CHARGE", "PENDING", null, response.errorMessage()
            );
            paymentTransactionRepository.save(tx);
            payment = paymentRepository.save(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            PaymentTransaction tx = new PaymentTransaction(
                    payment, null, "CHARGE", "DECLINED", response.errorCode(), response.errorMessage()
            );
            paymentTransactionRepository.save(tx);
            payment = paymentRepository.save(payment);

            outboxService.recordEvent(
                    "Payment",
                    payment.getId().toString(),
                    "PaymentFailedEvent",
                    "payment.events",
                    payment.getId().toString(),
                    PaymentFailedEvent.of(payment.getId(), ride.getId(), response.errorCode(), response.errorMessage())
            );
        }

        return payment;
    }

    public Payment chargeFallback(Payment payment, Ride ride, Throwable throwable) {
        log.warn("Payment charge fallback triggered for ride {} due to: {}", ride.getId(), throwable.getMessage());

        // Safe Fallback: mark as RECONCILIATION_REQUIRED so background reconciler checks status later
        payment.setStatus(PaymentStatus.RECONCILIATION_REQUIRED);
        PaymentTransaction tx = new PaymentTransaction(
                payment, null, "CHARGE", "FALLBACK_RECONCILE",
                "RESILIENCE_FALLBACK", throwable.getMessage()
        );
        paymentTransactionRepository.save(tx);
        return paymentRepository.save(payment);
    }

    @Transactional
    public Payment refundPayment(UUID paymentId, BigDecimal refundAmount) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if (payment.getStatus() != PaymentStatus.SUCCEEDED) {
            throw new BusinessRuleException("Only SUCCEEDED payments can be refunded");
        }

        PaymentGatewayResponse response = paymentGateway.refund(payment.getId().toString(), refundAmount);
        payment.setStatus(PaymentStatus.REFUNDED);

        PaymentTransaction tx = new PaymentTransaction(
                payment, response.transactionReference(), "REFUND", "SUCCESS", null, null
        );
        paymentTransactionRepository.save(tx);

        return paymentRepository.save(payment);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));
    }
}
