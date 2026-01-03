package com.onlyspans.eventlogs.storage;

import com.onlyspans.eventlogs.entity.SettingsEntity;
import com.onlyspans.eventlogs.repository.SettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsStorageTest {

    @Mock
    private SettingsRepository settingsRepository;

    private SettingsStorage settingsStorage;

    @BeforeEach
    void setUp() {
        settingsStorage = new SettingsStorage(settingsRepository);
    }

    @Test
    void getSettings_shouldReturnSettingsWhenExists() {
        // Given
        SettingsEntity entity = new SettingsEntity(
            "global",
            30,
            Instant.now(),
            "test-user"
        );
        when(settingsRepository.findById("global")).thenReturn(Optional.of(entity));

        // When
        Optional<SettingsEntity> result = settingsStorage.getSettings();

        // Then
        assertTrue(result.isPresent());
        assertEquals(entity, result.get());
        verify(settingsRepository).findById("global");
    }

    @Test
    void getSettings_shouldReturnEmptyWhenNotExists() {
        // Given
        when(settingsRepository.findById("global")).thenReturn(Optional.empty());

        // When
        Optional<SettingsEntity> result = settingsStorage.getSettings();

        // Then
        assertFalse(result.isPresent());
        verify(settingsRepository).findById("global");
    }

    @Test
    void getSettings_shouldReturnEmptyOnException() {
        // Given
        when(settingsRepository.findById("global")).thenThrow(new RuntimeException("Database error"));

        // When
        Optional<SettingsEntity> result = settingsStorage.getSettings();

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void saveSettings_shouldSaveEntity() {
        // Given
        SettingsEntity entity = new SettingsEntity(
            "global",
            60,
            Instant.now(),
            "api-user"
        );
        when(settingsRepository.save(any(SettingsEntity.class))).thenReturn(entity);

        // When
        settingsStorage.saveSettings(entity);

        // Then
        verify(settingsRepository).save(entity);
    }

    @Test
    void saveSettings_shouldThrowRuntimeExceptionOnError() {
        // Given
        SettingsEntity entity = new SettingsEntity(
            "global",
            60,
            Instant.now(),
            "api-user"
        );
        when(settingsRepository.save(any(SettingsEntity.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When/Then
        assertThrows(RuntimeException.class, () -> settingsStorage.saveSettings(entity));
    }
}
