package com.rideapp.booking.entity;

import com.rideapp.common.exception.InvalidStateTransitionException;
import com.rideapp.driver.entity.Driver;
import com.rideapp.pricing.entity.FareBreakdown;
import com.rideapp.user.entity.User;
import jakarta.persistence.*;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rides")
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(name = "shared_group_id")
    private UUID sharedGroupId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_type", nullable = false)
    private RideType rideType = RideType.SOLO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status = RideStatus.REQUESTED;

    @Column(name = "pickup_address", nullable = false)
    private String pickupAddress;

    @Column(name = "pickup_geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point pickupGeom;

    @Column(name = "dropoff_address", nullable = false)
    private String dropoffAddress;

    @Column(name = "dropoff_geom", columnDefinition = "geometry(Point, 4326)", nullable = false)
    private Point dropoffGeom;

    @Column(name = "estimated_distance_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal estimatedDistanceKm;

    @Column(name = "estimated_duration_min", nullable = false)
    private int estimatedDurationMin;

    @Column(name = "actual_distance_km", precision = 8, scale = 2)
    private BigDecimal actualDistanceKm;

    @Column(name = "actual_duration_min")
    private Integer actualDurationMin;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @OneToOne(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    private FareBreakdown fareBreakdown;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Ride() {
    }

    public void transitionTo(RideStatus targetStatus) {
        if (!this.status.canTransitionTo(targetStatus)) {
            throw new InvalidStateTransitionException(
                    String.format("Invalid ride transition from %s to %s for ride %s", this.status, targetStatus, this.id)
            );
        }
        this.status = targetStatus;
        if (targetStatus == RideStatus.IN_PROGRESS) {
            this.startedAt = Instant.now();
        } else if (targetStatus == RideStatus.COMPLETED) {
            this.completedAt = Instant.now();
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getPassenger() {
        return passenger;
    }

    public void setPassenger(User passenger) {
        this.passenger = passenger;
    }

    public Driver getDriver() {
        return driver;
    }

    public void setDriver(Driver driver) {
        this.driver = driver;
    }

    public UUID getSharedGroupId() {
        return sharedGroupId;
    }

    public void setSharedGroupId(UUID sharedGroupId) {
        this.sharedGroupId = sharedGroupId;
    }

    public RideType getRideType() {
        return rideType;
    }

    public void setRideType(RideType rideType) {
        this.rideType = rideType;
    }

    public RideStatus getStatus() {
        return status;
    }

    public void setStatus(RideStatus status) {
        this.status = status;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public Point getPickupGeom() {
        return pickupGeom;
    }

    public void setPickupGeom(Point pickupGeom) {
        this.pickupGeom = pickupGeom;
    }

    public String getDropoffAddress() {
        return dropoffAddress;
    }

    public void setDropoffAddress(String dropoffAddress) {
        this.dropoffAddress = dropoffAddress;
    }

    public Point getDropoffGeom() {
        return dropoffGeom;
    }

    public void setDropoffGeom(Point dropoffGeom) {
        this.dropoffGeom = dropoffGeom;
    }

    public BigDecimal getEstimatedDistanceKm() {
        return estimatedDistanceKm;
    }

    public void setEstimatedDistanceKm(BigDecimal estimatedDistanceKm) {
        this.estimatedDistanceKm = estimatedDistanceKm;
    }

    public int getEstimatedDurationMin() {
        return estimatedDurationMin;
    }

    public void setEstimatedDurationMin(int estimatedDurationMin) {
        this.estimatedDurationMin = estimatedDurationMin;
    }

    public BigDecimal getActualDistanceKm() {
        return actualDistanceKm;
    }

    public void setActualDistanceKm(BigDecimal actualDistanceKm) {
        this.actualDistanceKm = actualDistanceKm;
    }

    public Integer getActualDurationMin() {
        return actualDurationMin;
    }

    public void setActualDurationMin(Integer actualDurationMin) {
        this.actualDurationMin = actualDurationMin;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public FareBreakdown getFareBreakdown() {
        return fareBreakdown;
    }

    public void setFareBreakdown(FareBreakdown fareBreakdown) {
        this.fareBreakdown = fareBreakdown;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
