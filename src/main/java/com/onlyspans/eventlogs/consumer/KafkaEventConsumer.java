package com.onlyspans.eventlogs.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.storage.IEventStorage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
public final class KafkaEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final IEventStorage eventStorage;
    private final ObjectMapper objectMapper;
    private final Counter eventsReceivedCounter;
    private final Counter batchesProcessedCounter;
    private final Counter eventsFailedCounter;

    @Autowired
    public KafkaEventConsumer(
            IEventStorage eventStorage,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.eventStorage = eventStorage;
        this.objectMapper = objectMapper;

        this.eventsReceivedCounter = Counter.builder("event_logs.kafka.received")
                .description("Total number of events received from Kafka")
                .register(meterRegistry);

        this.batchesProcessedCounter = Counter.builder("event_logs.kafka.batches_processed")
                .description("Total number of batches processed")
                .register(meterRegistry);

        this.eventsFailedCounter = Counter.builder("event_logs.kafka.failed")
                .description("Total number of events that failed to process")
                .register(meterRegistry);
    }

    @KafkaListener(
        topics = "${kafka.topic.events:event-logs}",
        groupId = "${kafka.consumer.group-id:event-logs-consumer-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvents(
        @Payload List<String> messages,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        Acknowledgment acknowledgment
    ) {
        if (messages == null || messages.isEmpty()) {
            logger.debug("Received empty batch from topic {}", topic);
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            return;
        }

        logger.info("Received batch of {} messages from topic {}", messages.size(), topic);
        eventsReceivedCounter.increment(messages.size());

        List<EventEntity> entities = new ArrayList<>();
        List<String> failedMessages = new ArrayList<>();

        // Parse all messages
        for (String message : messages) {
            try {
                EventDto eventDto = objectMapper.readValue(message, EventDto.class);
                EventEntity eventEntity = convertToEntity(eventDto);
                entities.add(eventEntity);
            } catch (Exception e) {
                logger.error("Failed to parse Kafka message: {}", message, e);
                failedMessages.add(message);
                eventsFailedCounter.increment();
            }
        }

        // If all messages failed to parse, don't acknowledge
        if (entities.isEmpty()) {
            logger.error("All {} messages in batch failed to parse", messages.size());
            throw new RuntimeException("Failed to parse entire batch");
        }

        // Write to OpenSearch
        try {
            eventStorage.add(entities);
            logger.info("Successfully wrote {} events to OpenSearch", entities.size());
            batchesProcessedCounter.increment();

            // Acknowledge only after successful write to OpenSearch
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                logger.debug("Acknowledged batch of {} messages", messages.size());
            }

            // Log failed messages for monitoring
            if (!failedMessages.isEmpty()) {
                logger.warn("Batch contained {} failed messages out of {} total. " +
                    "Successfully processed {} messages.",
                    failedMessages.size(), messages.size(), entities.size());
            }
        } catch (Exception e) {
            logger.error("Failed to write batch to OpenSearch. Batch size: {}. " +
                "Will not acknowledge to trigger reprocessing.", entities.size(), e);
            throw new RuntimeException("Failed to write events to OpenSearch", e);
        }
    }

    private EventEntity convertToEntity(EventDto dto) {
        EventEntity entity = new EventEntity();
        entity.setId(dto.getId());
        entity.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());
        entity.setUser(dto.getUser());
        entity.setCategory(dto.getCategory());
        entity.setAction(dto.getAction());
        entity.setDocumentName(dto.getDocument());
        entity.setProject(dto.getProject());
        entity.setEnvironment(dto.getEnvironment());
        entity.setTenant(dto.getTenant());

        if (dto.getDetails() != null) {
            EventEntity.EventDetails details = new EventEntity.EventDetails();
            details.setIpAddress(dto.getDetails().getIpAddress());
            details.setUserAgent(dto.getDetails().getUserAgent());
            details.setAdditionalInfo(dto.getDetails().getAdditionalInfo());

            if (dto.getDetails().getChanges() != null) {
                List<EventEntity.Change> changes = dto.getDetails().getChanges().stream()
                    .map(changeDto -> {
                        EventEntity.Change change = new EventEntity.Change();
                        change.setField(changeDto.getField());
                        change.setOldValue(changeDto.getOldValue());
                        change.setNewValue(changeDto.getNewValue());
                        return change;
                    })
                    .toList();
                details.setChanges(changes);
            }

            entity.setDetails(details);
        }

        return entity;
    }
}
