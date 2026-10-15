package com.rideapp.driver.repository;

import com.rideapp.driver.entity.Driver;
import com.rideapp.driver.entity.OperationalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DriverRepository extends JpaRepository<Driver, UUID> {
    Optional<Driver> findByLicenseNumber(String licenseNumber);

    List<Driver> findByOperationalStatus(OperationalStatus status);

    @Query(value = """
        SELECT d.* FROM drivers d
        WHERE d.operational_status = 'AVAILABLE'
          AND d.current_location IS NOT NULL
          AND ST_DWithin(d.current_location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography, :radiusMeters)
        ORDER BY ST_Distance(d.current_location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography) ASC
        LIMIT :limit
        """, nativeQuery = true)
    List<Driver> findNearbyAvailableDrivers(
            @Param("lon") double lon,
            @Param("lat") double lat,
            @Param("radiusMeters") double radiusMeters,
            @Param("limit") int limit);
}
