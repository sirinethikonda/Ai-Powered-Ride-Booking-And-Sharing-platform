package com.rideapp.sharedride.entity;

import com.rideapp.user.entity.Gender;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shared_ride_groups")
public class SharedRideGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharedGroupStatus status = SharedGroupStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_policy", nullable = false)
    private GenderMatchingPolicy genderPolicy = GenderMatchingPolicy.STRICT_GENDER_COMPATIBLE;

    @Enumerated(EnumType.STRING)
    @Column(name = "allowed_gender", nullable = false)
    private Gender allowedGender;

    @Column(name = "max_detour_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxDetourPercent = new BigDecimal("30.00");

    @OneToMany(mappedBy = "sharedGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RidePassenger> passengers = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SharedRideGroup() {
    }

    public SharedRideGroup(Gender allowedGender, GenderMatchingPolicy genderPolicy, BigDecimal maxDetourPercent) {
        this.allowedGender = allowedGender;
        this.genderPolicy = genderPolicy;
        this.maxDetourPercent = maxDetourPercent != null ? maxDetourPercent : new BigDecimal("30.00");
        this.status = SharedGroupStatus.OPEN;
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

    public SharedGroupStatus getStatus() {
        return status;
    }

    public void setStatus(SharedGroupStatus status) {
        this.status = status;
    }

    public GenderMatchingPolicy getGenderPolicy() {
        return genderPolicy;
    }

    public void setGenderPolicy(GenderMatchingPolicy genderPolicy) {
        this.genderPolicy = genderPolicy;
    }

    public Gender getAllowedGender() {
        return allowedGender;
    }

    public void setAllowedGender(Gender allowedGender) {
        this.allowedGender = allowedGender;
    }

    public BigDecimal getMaxDetourPercent() {
        return maxDetourPercent;
    }

    public void setMaxDetourPercent(BigDecimal maxDetourPercent) {
        this.maxDetourPercent = maxDetourPercent;
    }

    public List<RidePassenger> getPassengers() {
        return passengers;
    }

    public void setPassengers(List<RidePassenger> passengers) {
        this.passengers = passengers;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
