package com.rideapp.sharedride.repository;

import com.rideapp.sharedride.entity.RidePassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RidePassengerRepository extends JpaRepository<RidePassenger, UUID> {
    List<RidePassenger> findBySharedGroupId(UUID sharedGroupId);
    Optional<RidePassenger> findByRideId(UUID rideId);
}
