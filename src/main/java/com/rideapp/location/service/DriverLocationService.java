package com.rideapp.location.service;

import com.rideapp.driver.entity.Driver;
import com.rideapp.driver.entity.OperationalStatus;
import com.rideapp.driver.entity.Vehicle;
import com.rideapp.driver.repository.DriverRepository;
import com.rideapp.driver.repository.VehicleRepository;
import com.rideapp.location.dto.DriverLocationUpdateRequest;
import com.rideapp.location.dto.NearbyDriverDto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DriverLocationService {

    private static final Logger log = LoggerFactory.getLogger(DriverLocationService.class);
    public static final String DRIVERS_GEO_KEY = "drivers:available:geo";

    private final StringRedisTemplate redisTemplate;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

    public DriverLocationService(
            StringRedisTemplate redisTemplate,
            DriverRepository driverRepository,
            VehicleRepository vehicleRepository) {
        this.redisTemplate = redisTemplate;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public void updateDriverLocation(UUID driverId, DriverLocationUpdateRequest request) {
        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver == null) {
            log.warn("Cannot update location for non-existent driver: {}", driverId);
            return;
        }

        // Update PostGIS persistent geometry
        org.locationtech.jts.geom.Point postgisPoint = geometryFactory.createPoint(
                new Coordinate(request.getLongitude(), request.getLatitude())
        );
        driver.setCurrentLocation(postgisPoint);
        driverRepository.save(driver);

        // If driver is currently AVAILABLE, update Redis GEO cache
        if (driver.getOperationalStatus() == OperationalStatus.AVAILABLE) {
            try {
                redisTemplate.opsForGeo().add(
                        DRIVERS_GEO_KEY,
                        new org.springframework.data.geo.Point(request.getLongitude(), request.getLatitude()),
                        driverId.toString()
                );
            } catch (Exception e) {
                log.error("Failed to update driver location in Redis GEO", e);
            }
        } else {
            removeDriverFromGeo(driverId);
        }
    }

    public void removeDriverFromGeo(UUID driverId) {
        try {
            redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY, driverId.toString());
        } catch (Exception e) {
            log.error("Failed to remove driver from Redis GEO: {}", driverId, e);
        }
    }

    public List<NearbyDriverDto> findNearbyAvailableDrivers(double longitude, double latitude, double radiusKm, int limit) {
        List<NearbyDriverDto> nearbyDrivers = new ArrayList<>();

        try {
            Circle circle = new Circle(
                    new org.springframework.data.geo.Point(longitude, latitude),
                    new Distance(radiusKm, Metrics.KILOMETERS)
            );
            RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                    .newGeoRadiusArgs()
                    .includeDistance()
                    .includeCoordinates()
                    .sortAscending()
                    .limit(limit);

            GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                    redisTemplate.opsForGeo().radius(DRIVERS_GEO_KEY, circle, args);

            if (results != null && !results.getContent().isEmpty()) {
                for (var result : results.getContent()) {
                    String driverIdStr = result.getContent().getName();
                    UUID driverId = UUID.fromString(driverIdStr);
                    double distance = result.getDistance().getValue();
                    var point = result.getContent().getPoint();

                    driverRepository.findById(driverId).ifPresent(driver -> {
                        if (driver.getOperationalStatus() == OperationalStatus.AVAILABLE) {
                            List<Vehicle> vehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(driverId);
                            var category = vehicles.isEmpty() ? null : vehicles.get(0).getCategory();

                            nearbyDrivers.add(new NearbyDriverDto(
                                    driverId,
                                    point.getY(),
                                    point.getX(),
                                    distance,
                                    driver.getRating(),
                                    category
                            ));
                        }
                    });
                }
                return nearbyDrivers;
            }
        } catch (Exception e) {
            log.warn("Redis GEO discovery failed, falling back to PostGIS spatial query: {}", e.getMessage());
        }

        // PostGIS fallback
        double radiusMeters = radiusKm * 1000.0;
        List<Driver> dbDrivers = driverRepository.findNearbyAvailableDrivers(longitude, latitude, radiusMeters, limit);
        for (Driver driver : dbDrivers) {
            if (driver.getCurrentLocation() != null) {
                List<Vehicle> vehicles = vehicleRepository.findByDriverIdAndIsActiveTrue(driver.getId());
                var category = vehicles.isEmpty() ? null : vehicles.get(0).getCategory();

                double distanceKm = calculateHaversineDistance(
                        latitude, longitude,
                        driver.getCurrentLocation().getY(), driver.getCurrentLocation().getX()
                );

                nearbyDrivers.add(new NearbyDriverDto(
                        driver.getId(),
                        driver.getCurrentLocation().getY(),
                        driver.getCurrentLocation().getX(),
                        distanceKm,
                        driver.getRating(),
                        category
                ));
            }
        }

        return nearbyDrivers;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
