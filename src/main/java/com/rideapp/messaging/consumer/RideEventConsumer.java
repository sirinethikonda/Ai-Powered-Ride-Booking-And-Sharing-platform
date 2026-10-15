package com.rideapp.messaging.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rideapp.booking.entity.RideType;
import com.rideapp.dispatch.service.DriverDispatchService;
import com.rideapp.messaging.dedup.MessageDeduplicationService;
import com.rideapp.sharedride.service.SharedRideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RideEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RideEventConsumer.class);
    private static final String CONSUMER_GROUP = "ride-event-consumer";

    private final ObjectMapper objectMapper;
    private final MessageDeduplicationService deduplicationService;
    private final DriverDispatchService driverDispatchService;
    private final SharedRideService sharedRideService;

    public RideEventConsumer(
            ObjectMapper objectMapper,
            MessageDeduplicationService deduplicationService,
            DriverDispatchService driverDispatchService,
            SharedRideService sharedRideService) {
        this.objectMapper = objectMapper;
        this.deduplicationService = deduplicationService;
        this.driverDispatchService = driverDispatchService;
        this.sharedRideService = sharedRideService;
    }

    @KafkaListener(topics = "ride.events", groupId = "ride-platform-group")
    public void consumeRideEvent(String messagePayload) {
        try {
            JsonNode root = objectMapper.readTree(messagePayload);
            String eventId = root.has("eventId") ? root.get("eventId").asText() : null;

            if (eventId != null && deduplicationService.isProcessed(eventId, CONSUMER_GROUP)) {
                log.info("Skipping duplicate ride event with ID: {}", eventId);
                return;
            }

            if (root.has("rideId")) {
                UUID rideId = UUID.fromString(root.get("rideId").asText());

                if (root.has("rideType")) {
                    String rideTypeStr = root.get("rideType").asText();
                    if (RideType.SHARED.name().equalsIgnoreCase(rideTypeStr)) {
                        log.info("Processing shared ride matching for ride {}", rideId);
                        try {
                            sharedRideService.matchOrEnqueueSharedRide(rideId);
                        } catch (Exception ex) {
                            log.error("Shared ride matching evaluation encountered an issue: {}", ex.getMessage());
                        }
                    }

                    // Automatically trigger driver dispatch
                    log.info("Triggering driver dispatch for requested ride {}", rideId);
                    driverDispatchService.dispatchRide(rideId);
                }
            }

            if (eventId != null) {
                deduplicationService.markProcessed(eventId, CONSUMER_GROUP);
            }
        } catch (Exception e) {
            log.error("Failed to process ride event message from Kafka: {}", messagePayload, e);
        }
    }
}
