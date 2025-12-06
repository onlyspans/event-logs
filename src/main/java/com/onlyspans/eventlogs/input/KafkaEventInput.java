package com.onlyspans.eventlogs.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.entity.EventEntity;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Component
public class KafkaEventInput implements IEventInput {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventInput.class);
    private final BlockingQueue<EventEntity> eventQueue = new LinkedBlockingQueue<>();
    private final ObjectMapper objectMapper;

    @Autowired
    public KafkaEventInput(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "${kafka.topic.events:event-logs}",
        groupId = "${kafka.consumer.group-id:event-logs-consumer-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeEvent(
        @Payload String message,
        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
        Acknowledgment acknowledgment
    ) {
        try {
            logger.debug("Received message from topic {}: {}", topic, message);
            
            EventDto eventDto = objectMapper.readValue(message, EventDto.class);
            EventEntity eventEntity = convertToEntity(eventDto);
            
            eventQueue.offer(eventEntity, 1, TimeUnit.SECONDS);
            
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
            
            logger.debug("Successfully processed event with ID: {}", eventEntity.getId());
        } catch (Exception e) {
            logger.error("Error processing Kafka message: {}", message, e);
            // In a production system, you might want to send to a dead letter queue
            // For now, we'll just log the error
            throw new RuntimeException("Failed to process Kafka message", e);
        }
    }

    @Override
    public List<EventEntity> read() {
        List<EventEntity> events = new ArrayList<>();
        eventQueue.drainTo(events);
        return events;
    }

    private EventEntity convertToEntity(EventDto dto) {
        EventEntity entity = new EventEntity();
        entity.setId(dto.getId());
        entity.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());
        entity.setUser(dto.getUser());
        entity.setCategory(dto.getCategory());
        entity.setAction(dto.getAction());
        entity.setDocument(dto.getDocument());
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

