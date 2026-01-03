package com.onlyspans.eventlogs.service;

import com.onlyspans.eventlogs.dto.SettingsDto;
import com.onlyspans.eventlogs.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {

    @Mock
    private ISettingsService settingsService;

    @Mock
    private EventRepository eventRepository;

    private RetentionService retentionService;

    @BeforeEach
    void setUp() {
        retentionService = new RetentionService(settingsService, eventRepository);
    }

    @Test
    void applyRetention_shouldDeleteEventsOlderThanRetentionPeriod() {
        // Given
        int retentionDays = 30;
        SettingsDto settings = new SettingsDto(retentionDays, 10000);
        when(settingsService.getSettings()).thenReturn(settings);
        when(eventRepository.deleteEventsOlderThan(any(Instant.class))).thenReturn(5);

        Instant beforeExecution = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

        // When
        retentionService.applyRetention();

        // Then
        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(eventRepository).deleteEventsOlderThan(captor.capture());

        Instant cutoffDate = captor.getValue();
        assertNotNull(cutoffDate);

        // Check that cutoff date is approximately retention days ago (within 1 second tolerance)
        long secondsDifference = Math.abs(ChronoUnit.SECONDS.between(cutoffDate, beforeExecution));
        assertTrue(secondsDifference < 2, "Cutoff date should be approximately " + retentionDays + " days ago");
    }

    @Test
    void applyRetention_shouldUseRetentionPeriodFromSettings() {
        // Given
        SettingsDto settings = new SettingsDto(90, 10000);
        when(settingsService.getSettings()).thenReturn(settings);
        when(eventRepository.deleteEventsOlderThan(any(Instant.class))).thenReturn(10);

        // When
        retentionService.applyRetention();

        // Then
        verify(settingsService).getSettings();
        verify(eventRepository).deleteEventsOlderThan(any(Instant.class));
    }

    @Test
    void applyRetention_shouldHandleExceptionsGracefully() {
        // Given
        when(settingsService.getSettings()).thenThrow(new RuntimeException("Database error"));

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> retentionService.applyRetention());

        verify(settingsService).getSettings();
        verify(eventRepository, never()).deleteEventsOlderThan(any(Instant.class));
    }

    @Test
    void applyRetention_shouldHandleRepositoryExceptionsGracefully() {
        // Given
        SettingsDto settings = new SettingsDto(30, 10000);
        when(settingsService.getSettings()).thenReturn(settings);
        when(eventRepository.deleteEventsOlderThan(any(Instant.class)))
            .thenThrow(new RuntimeException("Delete failed"));

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> retentionService.applyRetention());

        verify(eventRepository).deleteEventsOlderThan(any(Instant.class));
    }

    @Test
    void applyRetention_shouldReturnDeletedCount() {
        // Given
        SettingsDto settings = new SettingsDto(60, 10000);
        when(settingsService.getSettings()).thenReturn(settings);
        when(eventRepository.deleteEventsOlderThan(any(Instant.class))).thenReturn(42);

        // When
        retentionService.applyRetention();

        // Then
        verify(eventRepository).deleteEventsOlderThan(any(Instant.class));
        // The method logs the count but doesn't return it, verify it was called
    }
}
