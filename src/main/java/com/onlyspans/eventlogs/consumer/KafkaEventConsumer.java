package com.onlyspans.eventlogs.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.service.IEventService;
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

import java.util.ArrayList;
import java.util.List;

@Component
public final class KafkaEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventConsumer.class);

    private final IEventService eventService;
    private final ObjectMapper objectMapper;
    private final Counter eventsReceivedCounter;
    private final Counter batchesProcessedCounter;
    private final Counter eventsFailedCounter;

    @Autowired
    public KafkaEventConsumer(
            IEventService eventService,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry
    ) {
        this.eventService = eventService;
        this.objectMapper = objectMapper;

        this.eventsReceivedCounter = Counter.builder("event_logs_received")
                .description("Total number of events received from Kafka")
                .register(meterRegistry);

        this.batchesProcessedCounter = Counter.builder("event_logs_batches_processed")
                .description("Total number of batches processed")
                .register(meterRegistry);

        this.eventsFailedCounter = Counter.builder("event_logs_failed")
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

        List<EventDto> eventDtos = new ArrayList<>();
        List<String> failedMessages = new ArrayList<>();

        for (String message : messages) {
            try {
                logger.debug("Attempting to parse message: {}", message);
                EventDto eventDto = objectMapper.readValue(message, EventDto.class);
                eventDtos.add(eventDto);
                logger.debug("Successfully parsed event with user: {}, category: {}",
                    eventDto.getUser(), eventDto.getCategory());
            } catch (Exception e) {
                logger.error("Failed to parse Kafka message. Message content: {}", message);
                logger.error("Parse error details:", e);
                failedMessages.add(message);
                eventsFailedCounter.increment();
            }
        }

        if (eventDtos.isEmpty()) {
            logger.error("All {} messages in batch failed to parse", messages.size());
            throw new RuntimeException("Failed to parse entire batch");
        }

        try {
            eventService.ingestEvents(eventDtos);
            logger.info("Successfully processed {} events", eventDtos.size());
            batchesProcessedCounter.increment();

            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                logger.debug("Acknowledged batch of {} messages", messages.size());
            }

            if (!failedMessages.isEmpty()) {
                logger.warn("Batch contained {} failed messages out of {} total. " +
                    "Successfully processed {} messages.",
                    failedMessages.size(), messages.size(), eventDtos.size());
            }
        } catch (Exception e) {
            logger.error("Failed to write batch to storage. Batch size: {}. " +
                "Will not acknowledge to trigger reprocessing.", eventDtos.size(), e);
            throw new RuntimeException("Failed to write events to storage", e);
        }
    }
}
