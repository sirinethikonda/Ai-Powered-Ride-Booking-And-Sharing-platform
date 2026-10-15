package com.rideapp.saga.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "saga_steps")
public class SagaStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "saga_instance_id", nullable = false)
    private SagaInstance sagaInstance;

    @Column(name = "step_name", nullable = false)
    private String stepName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SagaStepStatus status = SagaStepStatus.PENDING;

    @Column(name = "error_details", columnDefinition = "text")
    private String errorDetails;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public SagaStep() {
    }

    public SagaStep(SagaInstance sagaInstance, String stepName) {
        this.sagaInstance = sagaInstance;
        this.stepName = stepName;
        this.status = SagaStepStatus.PENDING;
    }

    @PrePersist
    protected void onCreate() {
        this.startedAt = Instant.now();
    }

    public void markSuccess() {
        this.status = SagaStepStatus.SUCCESS;
        this.finishedAt = Instant.now();
    }

    public void markFailed(String errorDetails) {
        this.status = SagaStepStatus.FAILED;
        this.errorDetails = errorDetails;
        this.finishedAt = Instant.now();
    }

    public void markCompensated() {
        this.status = SagaStepStatus.COMPENSATED;
        this.finishedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SagaInstance getSagaInstance() {
        return sagaInstance;
    }

    public void setSagaInstance(SagaInstance sagaInstance) {
        this.sagaInstance = sagaInstance;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public SagaStepStatus getStatus() {
        return status;
    }

    public void setStatus(SagaStepStatus status) {
        this.status = status;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
