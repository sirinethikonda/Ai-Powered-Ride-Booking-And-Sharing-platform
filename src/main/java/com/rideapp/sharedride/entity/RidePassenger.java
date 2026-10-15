package com.rideapp.sharedride.entity;

import com.rideapp.booking.entity.Ride;
import com.rideapp.user.entity.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ride_passengers")
public class RidePassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_group_id", nullable = false)
    private SharedRideGroup sharedGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    @Column(name = "seats_reserved", nullable = false)
    private int seatsReserved = 1;

    @Column(name = "pickup_sequence", nullable = false)
    private int pickupSequence = 1;

    @Column(name = "dropoff_sequence", nullable = false)
    private int dropoffSequence = 1;

    @Column(name = "individual_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal individualFare;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public RidePassenger() {
    }

    public RidePassenger(SharedRideGroup sharedGroup, Ride ride, User passenger, int seatsReserved,
                         int pickupSequence, int dropoffSequence, BigDecimal individualFare) {
        this.sharedGroup = sharedGroup;
        this.ride = ride;
        this.passenger = passenger;
        this.seatsReserved = seatsReserved;
        this.pickupSequence = pickupSequence;
        this.dropoffSequence = dropoffSequence;
        this.individualFare = individualFare;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public SharedRideGroup getSharedGroup() {
        return sharedGroup;
    }

    public void setSharedGroup(SharedRideGroup sharedGroup) {
        this.sharedGroup = sharedGroup;
    }

    public Ride getRide() {
        return ride;
    }

    public void setRide(Ride ride) {
        this.ride = ride;
    }

    public User getPassenger() {
        return passenger;
    }

    public void setPassenger(User passenger) {
        this.passenger = passenger;
    }

    public int getSeatsReserved() {
        return seatsReserved;
    }

    public void setSeatsReserved(int seatsReserved) {
        this.seatsReserved = seatsReserved;
    }

    public int getPickupSequence() {
        return pickupSequence;
    }

    public void setPickupSequence(int pickupSequence) {
        this.pickupSequence = pickupSequence;
    }

    public int getDropoffSequence() {
        return dropoffSequence;
    }

    public void setDropoffSequence(int dropoffSequence) {
        this.dropoffSequence = dropoffSequence;
    }

    public BigDecimal getIndividualFare() {
        return individualFare;
    }

    public void setIndividualFare(BigDecimal individualFare) {
        this.individualFare = individualFare;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
