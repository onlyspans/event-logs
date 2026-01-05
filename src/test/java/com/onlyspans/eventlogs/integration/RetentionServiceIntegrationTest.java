package com.onlyspans.eventlogs.integration;

import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.entity.SettingsEntity;
import com.onlyspans.eventlogs.repository.EventRepository;
import com.onlyspans.eventlogs.repository.SettingsRepository;
import com.onlyspans.eventlogs.service.RetentionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RetentionServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private SettingsRepository settingsRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        settingsRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        eventRepository.deleteAll();
        settingsRepository.deleteAll();
    }

    @Test
    void applyRetention_shouldDeleteOldEventsBasedOnRetentionPeriod() {
        // Given - set retention to 30 days
        SettingsEntity settings = new SettingsEntity("global", 30, Instant.now(), "test");
        settingsRepository.save(settings);

        // Create events
        Instant now = Instant.now();
        createEventWithTimestamp(now.minus(40, ChronoUnit.DAYS)); // Should be deleted
        createEventWithTimestamp(now.minus(35, ChronoUnit.DAYS)); // Should be deleted
        createEventWithTimestamp(now.minus(25, ChronoUnit.DAYS)); // Should be kept
        createEventWithTimestamp(now.minus(10, ChronoUnit.DAYS)); // Should be kept
        createEventWithTimestamp(now); // Should be kept

        // When
        retentionService.applyRetention();

        // Then
        List<EventEntity> remaining = eventRepository.findAll();
        assertEquals(3, remaining.size());
        assertTrue(remaining.stream()
                .allMatch(e -> e.getTimestamp().isAfter(now.minus(31, ChronoUnit.DAYS))));
    }

    @Test
    void applyRetention_shouldUseDefaultRetentionWhenNoSettingsExist() {
        // Given - no settings (default 90 days)
        Instant now = Instant.now();
        createEventWithTimestamp(now.minus(100, ChronoUnit.DAYS)); // Should be deleted
        createEventWithTimestamp(now.minus(80, ChronoUnit.DAYS)); // Should be kept
        createEventWithTimestamp(now.minus(50, ChronoUnit.DAYS)); // Should be kept
        createEventWithTimestamp(now); // Should be kept

        // When
        retentionService.applyRetention();

        // Then
        List<EventEntity> remaining = eventRepository.findAll();
        assertEquals(3, remaining.size());
    }

    @Test
    void applyRetention_shouldNotDeleteRecentEvents() {
        // Given
        SettingsEntity settings = new SettingsEntity("global", 7, Instant.now(), "test");
        settingsRepository.save(settings);

        Instant now = Instant.now();
        createEventWithTimestamp(now.minus(1, ChronoUnit.DAYS));
        createEventWithTimestamp(now.minus(2, ChronoUnit.DAYS));
        createEventWithTimestamp(now.minus(3, ChronoUnit.DAYS));

        int initialCount = eventRepository.findAll().size();

        // When
        retentionService.applyRetention();

        // Then
        List<EventEntity> remaining = eventRepository.findAll();
        assertEquals(initialCount, remaining.size());
    }

    @Test
    void applyRetention_shouldHandleEmptyDatabase() {
        // Given - empty database

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> retentionService.applyRetention());
    }

    @Test
    void applyRetention_shouldDeleteAllEventsIfAllExpired() {
        // Given
        SettingsEntity settings = new SettingsEntity("global", 1, Instant.now(), "test");
        settingsRepository.save(settings);

        Instant now = Instant.now();
        createEventWithTimestamp(now.minus(10, ChronoUnit.DAYS));
        createEventWithTimestamp(now.minus(5, ChronoUnit.DAYS));
        createEventWithTimestamp(now.minus(2, ChronoUnit.DAYS));

        // When
        retentionService.applyRetention();

        // Then
        List<EventEntity> remaining = eventRepository.findAll();
        assertEquals(0, remaining.size());
    }

    @Test
    void applyRetention_shouldWorkWithDifferentRetentionPeriods() {
        // Given - retention 60 days
        SettingsEntity settings = new SettingsEntity("global", 60, Instant.now(), "test");
        settingsRepository.save(settings);

        Instant now = Instant.now();
        createEventWithTimestamp(now.minus(70, ChronoUnit.DAYS)); // Should be deleted
        createEventWithTimestamp(now.minus(65, ChronoUnit.DAYS)); // Should be deleted
        createEventWithTimestamp(now.minus(55, ChronoUnit.DAYS)); // Should be kept
        createEventWithTimestamp(now.minus(30, ChronoUnit.DAYS)); // Should be kept
        createEventWithTimestamp(now.minus(1, ChronoUnit.DAYS)); // Should be kept

        // When
        retentionService.applyRetention();

        // Then
        List<EventEntity> remaining = eventRepository.findAll();
        assertEquals(3, remaining.size());
    }

    @Test
    void applyRetention_shouldPreserveEventsOnRetentionBoundary() {
        // Given - retention 30 days
        SettingsEntity settings = new SettingsEntity("global", 30, Instant.now(), "test");
        settingsRepository.save(settings);

        Instant now = Instant.now();
        Instant exactBoundary = now.minus(30, ChronoUnit.DAYS);

        createEventWithTimestamp(exactBoundary.minusSeconds(1)); // Should be deleted
        createEventWithTimestamp(exactBoundary); // Boundary case
        createEventWithTimestamp(exactBoundary.plusSeconds(1)); // Should be kept

        // When
        retentionService.applyRetention();

        // Then
        List<EventEntity> remaining = eventRepository.findAll();
        assertTrue(remaining.size() >= 1);
        assertTrue(remaining.stream()
                .allMatch(e -> !e.getTimestamp().isBefore(exactBoundary)));
    }

    @Test
    void applyRetention_shouldHandleLargeNumberOfEvents() {
        // Given - retention 10 days
        SettingsEntity settings = new SettingsEntity("global", 10, Instant.now(), "test");
        settingsRepository.save(settings);

        Instant now = Instant.now();
        // Create 50 old events and 50 recent events
        for (int i = 0; i < 50; i++) {
            createEventWithTimestamp(now.minus(20 + i, ChronoUnit.DAYS)); // Old
            createEventWithTimestamp(now.minus(i, ChronoUnit.HOURS)); // Recent
        }

        // When
        retentionService.applyRetention();

        // Then
        List<EventEntity> remaining = eventRepository.findAll();
        assertEquals(50, remaining.size());
        assertTrue(remaining.stream()
                .allMatch(e -> e.getTimestamp().isAfter(now.minus(11, ChronoUnit.DAYS))));
    }

    private EventEntity createEventWithTimestamp(Instant timestamp) {
        EventEntity event = new EventEntity();
        event.setId(UUID.randomUUID());
        event.setTimestamp(timestamp);
        event.setUser("test-user");
        event.setCategory("test-category");
        event.setAction("test-action");
        event.setDocumentName("test-doc");
        event.setProject("test-project");
        event.setEnvironment("test-env");
        event.setTenant("test-tenant");
        return eventRepository.save(event);
    }
}
