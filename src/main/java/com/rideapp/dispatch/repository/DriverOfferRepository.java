package com.rideapp.dispatch.repository;

import com.rideapp.dispatch.entity.DriverOffer;
import com.rideapp.dispatch.entity.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverOfferRepository extends JpaRepository<DriverOffer, UUID> {
    List<DriverOffer> findByRideId(UUID rideId);

    List<DriverOffer> findByDriverIdAndStatus(UUID driverId, OfferStatus status);

    @Query("SELECT o FROM DriverOffer o WHERE o.status = 'OFFERED' AND o.expiresAt < :now")
    List<DriverOffer> findExpiredOffers(@Param("now") Instant now);

    @Query("SELECT o.driver.id FROM DriverOffer o WHERE o.ride.id = :rideId")
    List<UUID> findDriverIdsAlreadyOfferedForRide(@Param("rideId") UUID rideId);

    Optional<DriverOffer> findByRideIdAndStatus(UUID rideId, OfferStatus status);
}
