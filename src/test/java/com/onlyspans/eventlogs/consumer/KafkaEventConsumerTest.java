package com.onlyspans.eventlogs.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.service.IEventService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaEventConsumerTest {

    @Mock
    private IEventService eventService;

    @Mock
    private Acknowledgment acknowledgment;

    private SimpleMeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private KafkaEventConsumer kafkaEventConsumer;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        meterRegistry = new SimpleMeterRegistry();

        kafkaEventConsumer = new KafkaEventConsumer(eventService, objectMapper, meterRegistry);
    }

    @Test
    void consumeEvents_shouldProcessValidMessages() throws Exception {
        // Given
        String message = createValidEventJson();
        List<String> messages = List.of(message);

        // When
        kafkaEventConsumer.consumeEvents(messages, "event-logs", acknowledgment);

        // Then
        ArgumentCaptor<List<EventDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventService).ingestEvents(captor.capture());

        List<EventDto> capturedDtos = captor.getValue();
        assertEquals(1, capturedDtos.size());

        EventDto dto = capturedDtos.get(0);
        assertEquals("test-user", dto.getUser());
        assertEquals("test-category", dto.getCategory());

        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEvents_shouldProcessMultipleMessages() throws Exception {
        // Given
        String message1 = createValidEventJson();
        String message2 = createValidEventJson();
        List<String> messages = List.of(message1, message2);

        // When
        kafkaEventConsumer.consumeEvents(messages, "event-logs", acknowledgment);

        // Then
        ArgumentCaptor<List<EventDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventService).ingestEvents(captor.capture());

        List<EventDto> capturedDtos = captor.getValue();
        assertEquals(2, capturedDtos.size());

        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEvents_shouldSkipInvalidMessagesButProcessValid() throws Exception {
        // Given
        String validMessage = createValidEventJson();
        String invalidMessage = "{invalid json}";
        List<String> messages = List.of(validMessage, invalidMessage);

        // When
        kafkaEventConsumer.consumeEvents(messages, "event-logs", acknowledgment);

        // Then
        ArgumentCaptor<List<EventDto>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventService).ingestEvents(captor.capture());

        List<EventDto> capturedDtos = captor.getValue();
        assertEquals(1, capturedDtos.size()); // Only valid message processed

        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEvents_shouldThrowExceptionWhenAllMessagesFail() {
        // Given
        String invalidMessage1 = "{invalid json}";
        String invalidMessage2 = "{also invalid}";
        List<String> messages = List.of(invalidMessage1, invalidMessage2);

        // When/Then
        assertThrows(RuntimeException.class, () ->
            kafkaEventConsumer.consumeEvents(messages, "event-logs", acknowledgment));

        verify(eventService, never()).ingestEvents(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeEvents_shouldThrowExceptionWhenStorageFails() throws Exception {
        // Given
        String message = createValidEventJson();
        List<String> messages = List.of(message);

        doThrow(new RuntimeException("Storage error"))
            .when(eventService).ingestEvents(any());

        // When/Then
        assertThrows(RuntimeException.class, () ->
            kafkaEventConsumer.consumeEvents(messages, "event-logs", acknowledgment));

        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void consumeEvents_shouldHandleEmptyBatch() {
        // Given
        List<String> messages = List.of();

        // When
        kafkaEventConsumer.consumeEvents(messages, "event-logs", acknowledgment);

        // Then
        verify(eventService, never()).ingestEvents(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEvents_shouldHandleNullBatch() {
        // When
        kafkaEventConsumer.consumeEvents(null, "event-logs", acknowledgment);

        // Then
        verify(eventService, never()).ingestEvents(any());
        verify(acknowledgment).acknowledge();
    }

    @Test
    void consumeEvents_shouldHandleNullAcknowledgment() throws Exception {
        // Given
        String message = createValidEventJson();
        List<String> messages = List.of(message);

        // When/Then - should not throw exception
        assertDoesNotThrow(() ->
            kafkaEventConsumer.consumeEvents(messages, "event-logs", null));

        verify(eventService).ingestEvents(any());
    }

    private String createValidEventJson() {
        return """
            {
                "timestamp": "%s",
                "user": "test-user",
                "category": "test-category",
                "action": "test-action",
                "documentName": "test-document",
                "project": "test-project",
                "environment": "test-env",
                "tenant": "test-tenant"
            }
            """.formatted(Instant.now().toString());
    }
}
