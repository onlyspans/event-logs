package com.onlyspans.eventlogs.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.repository.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@EmbeddedKafka(
        partitions = 1,
        topics = {"event-logs"},
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:9092",
                "port=9092"
        }
)
@DirtiesContext
class KafkaEventConsumerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TOPIC = "event-logs";

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        eventRepository.deleteAll();
    }

    @Test
    void shouldConsumeAndStoreEventFromKafka() throws Exception {
        // Given
        EventDto eventDto = createEventDto("kafka-user", "kafka-category", "kafka-action");
        String message = objectMapper.writeValueAsString(eventDto);

        // When
        kafkaTemplate.send(TOPIC, message);

        // Then - wait for message to be consumed and stored
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EventEntity> events = eventRepository.findAll();
            assertEquals(1, events.size());

            EventEntity stored = events.get(0);
            assertEquals("kafka-user", stored.getUser());
            assertEquals("kafka-category", stored.getCategory());
            assertEquals("kafka-action", stored.getAction());
        });
    }

    @Test
    void shouldConsumeMultipleEventsFromKafka() throws Exception {
        // Given
        EventDto event1 = createEventDto("user1", "cat1", "act1");
        EventDto event2 = createEventDto("user2", "cat2", "act2");
        EventDto event3 = createEventDto("user3", "cat3", "act3");

        // When
        kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(event1));
        kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(event2));
        kafkaTemplate.send(TOPIC, objectMapper.writeValueAsString(event3));

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EventEntity> events = eventRepository.findAll();
            assertEquals(3, events.size());
        });
    }

    @Test
    void shouldStoreEventWithAllFields() throws Exception {
        // Given
        EventDto eventDto = new EventDto();
        eventDto.setTimestamp(Instant.now());
        eventDto.setUser("test-user");
        eventDto.setCategory("test-category");
        eventDto.setAction("test-action");
        eventDto.setDocument("test-document");
        eventDto.setProject("test-project");
        eventDto.setEnvironment("test-environment");
        eventDto.setTenant("test-tenant");
        eventDto.setCorrelationId("corr-123");
        eventDto.setTraceId("trace-456");

        EventDto.EventDetailsDto details = new EventDto.EventDetailsDto();
        details.setIpAddress("192.168.1.1");
        details.setUserAgent("Mozilla/5.0");
        details.setAdditionalInfo("Some additional info");
        eventDto.setDetails(details);

        String message = objectMapper.writeValueAsString(eventDto);

        // When
        kafkaTemplate.send(TOPIC, message);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EventEntity> events = eventRepository.findAll();
            assertEquals(1, events.size());

            EventEntity stored = events.get(0);
            assertEquals("test-user", stored.getUser());
            assertEquals("test-category", stored.getCategory());
            assertEquals("test-action", stored.getAction());
            assertEquals("test-document", stored.getDocumentName());
            assertEquals("test-project", stored.getProject());
            assertEquals("test-environment", stored.getEnvironment());
            assertEquals("test-tenant", stored.getTenant());
            assertEquals("corr-123", stored.getCorrelationId());
            assertEquals("trace-456", stored.getTraceId());

            assertNotNull(stored.getDetails());
            assertEquals("192.168.1.1", stored.getDetails().getIpAddress());
            assertEquals("Mozilla/5.0", stored.getDetails().getUserAgent());
            assertEquals("Some additional info", stored.getDetails().getAdditionalInfo());
        });
    }

    @Test
    void shouldHandleEventWithChangesInDetails() throws Exception {
        // Given
        EventDto eventDto = createEventDto("user", "document-update", "modify");

        EventDto.EventDetailsDto details = new EventDto.EventDetailsDto();
        EventDto.ChangeDto change1 = new EventDto.ChangeDto();
        change1.setField("status");
        change1.setOldValue("draft");
        change1.setNewValue("published");

        EventDto.ChangeDto change2 = new EventDto.ChangeDto();
        change2.setField("author");
        change2.setOldValue("john");
        change2.setNewValue("jane");

        details.setChanges(List.of(change1, change2));
        eventDto.setDetails(details);

        String message = objectMapper.writeValueAsString(eventDto);

        // When
        kafkaTemplate.send(TOPIC, message);

        // Then
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EventEntity> events = eventRepository.findAll();
            assertEquals(1, events.size());

            EventEntity stored = events.get(0);
            assertNotNull(stored.getDetails());
            assertNotNull(stored.getDetails().getChanges());
            assertEquals(2, stored.getDetails().getChanges().size());

            EventEntity.Change storedChange1 = stored.getDetails().getChanges().get(0);
            assertEquals("status", storedChange1.getField());
            assertEquals("draft", storedChange1.getOldValue());
            assertEquals("published", storedChange1.getNewValue());
        });
    }

    @Test
    void shouldSkipInvalidMessagesButProcessValid() throws Exception {
        // Given
        String invalidMessage = "{invalid json}";
        EventDto validEvent = createEventDto("valid-user", "valid-cat", "valid-act");
        String validMessage = objectMapper.writeValueAsString(validEvent);

        // When
        kafkaTemplate.send(TOPIC, invalidMessage);
        kafkaTemplate.send(TOPIC, validMessage);

        // Then - only valid message should be stored
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EventEntity> events = eventRepository.findAll();
            assertEquals(1, events.size());
            assertEquals("valid-user", events.get(0).getUser());
        });
    }

    @Test
    void shouldSetTimestampIfNotProvided() throws Exception {
        // Given
        EventDto eventDto = createEventDto("user", "cat", "act");
        eventDto.setTimestamp(null);
        String message = objectMapper.writeValueAsString(eventDto);

        Instant before = Instant.now();

        // When
        kafkaTemplate.send(TOPIC, message);

        // Then
        Instant after = Instant.now();
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<EventEntity> events = eventRepository.findAll();
            assertEquals(1, events.size());

            EventEntity stored = events.get(0);
            assertNotNull(stored.getTimestamp());
            assertTrue(stored.getTimestamp().isAfter(before.minusSeconds(1)));
            assertTrue(stored.getTimestamp().isBefore(after.plusSeconds(10)));
        });
    }

    private EventDto createEventDto(String user, String category, String action) {
        EventDto dto = new EventDto();
        dto.setTimestamp(Instant.now());
        dto.setUser(user);
        dto.setCategory(category);
        dto.setAction(action);
        dto.setDocument("test-doc");
        dto.setProject("test-project");
        dto.setEnvironment("test-env");
        dto.setTenant("test-tenant");
        return dto;
    }
}
