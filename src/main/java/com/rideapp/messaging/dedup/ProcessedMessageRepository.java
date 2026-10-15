package com.rideapp.messaging.dedup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, String> {
    Optional<ProcessedMessage> findByMessageIdAndConsumerGroup(String messageId, String consumerGroup);
}
