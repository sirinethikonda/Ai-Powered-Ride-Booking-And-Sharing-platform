package com.rideapp.pricing.repository;

import com.rideapp.pricing.entity.FareBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FareBreakdownRepository extends JpaRepository<FareBreakdown, UUID> {
    Optional<FareBreakdown> findByRideId(UUID rideId);
}
