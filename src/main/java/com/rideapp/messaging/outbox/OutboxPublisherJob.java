package com.rideapp.messaging.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisherJob {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherJob.class);

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherJob(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxRepository.findPendingEventsForPublishing(25);
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                // Send synchronously or wait for ack to confirm broker commit
                kafkaTemplate.send(event.getTopic(), event.getPartitionKey(), event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                event.setStatus(OutboxStatus.PUBLISHED);
                                event.setPublishedAt(Instant.now());
                                outboxRepository.save(event);
                                log.debug("Published outbox event {} to Kafka topic {}", event.getId(), event.getTopic());
                            } else {
                                handlePublishFailure(event, ex);
                            }
                        });
            } catch (Exception ex) {
                handlePublishFailure(event, ex);
            }
        }
    }

    private void handlePublishFailure(OutboxEvent event, Throwable ex) {
        log.error("Failed to publish outbox event {} to Kafka topic {}: {}", event.getId(), event.getTopic(), ex.getMessage());
        event.setRetryCount(event.getRetryCount() + 1);
        if (event.getRetryCount() >= 5) {
            event.setStatus(OutboxStatus.FAILED);
        }
        outboxRepository.save(event);
    }
}
