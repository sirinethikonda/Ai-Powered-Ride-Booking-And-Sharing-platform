package com.rideapp.messaging.dedup;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageDeduplicationService {

    private final ProcessedMessageRepository processedMessageRepository;

    public MessageDeduplicationService(ProcessedMessageRepository processedMessageRepository) {
        this.processedMessageRepository = processedMessageRepository;
    }

    @Transactional(readOnly = true)
    public boolean isProcessed(String messageId, String consumerGroup) {
        return processedMessageRepository.findByMessageIdAndConsumerGroup(messageId, consumerGroup).isPresent();
    }

    @Transactional
    public void markProcessed(String messageId, String consumerGroup) {
        ProcessedMessage message = new ProcessedMessage(messageId, consumerGroup);
        processedMessageRepository.save(message);
    }
}
