package com.onlyspans.eventlogs.service;

import com.onlyspans.eventlogs.dto.SettingsDto;
import com.onlyspans.eventlogs.entity.SettingsEntity;
import com.onlyspans.eventlogs.storage.ISettingsStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private ISettingsStorage settingsStorage;

    private SettingsService settingsService;

    private static final int DEFAULT_RETENTION_DAYS = 90;
    private static final int DEFAULT_MAX_EXPORT_SIZE = 10000;

    @BeforeEach
    void setUp() {
        settingsService = new SettingsService(settingsStorage);
        ReflectionTestUtils.setField(settingsService, "defaultRetentionPeriodDays", DEFAULT_RETENTION_DAYS);
        ReflectionTestUtils.setField(settingsService, "defaultMaxExportSize", DEFAULT_MAX_EXPORT_SIZE);
    }

    @Test
    void getSettings_shouldReturnStoredSettings() {
        // Given
        SettingsEntity entity = new SettingsEntity(
            "global",
            30,
            Instant.now(),
            "test-user"
        );
        when(settingsStorage.getSettings()).thenReturn(Optional.of(entity));

        // When
        SettingsDto result = settingsService.getSettings();

        // Then
        assertNotNull(result);
        assertEquals(30, result.getRetentionPeriodDays());
        assertEquals(DEFAULT_MAX_EXPORT_SIZE, result.getMaxExportSize());
    }

    @Test
    void getSettings_shouldReturnDefaultsWhenNoStoredSettings() {
        // Given
        when(settingsStorage.getSettings()).thenReturn(Optional.empty());

        // When
        SettingsDto result = settingsService.getSettings();

        // Then
        assertNotNull(result);
        assertEquals(DEFAULT_RETENTION_DAYS, result.getRetentionPeriodDays());
        assertEquals(DEFAULT_MAX_EXPORT_SIZE, result.getMaxExportSize());
    }

    @Test
    void updateSettings_shouldSaveToStorage() {
        // Given
        SettingsDto dto = new SettingsDto(60, DEFAULT_MAX_EXPORT_SIZE);

        // When
        SettingsDto result = settingsService.updateSettings(dto);

        // Then
        ArgumentCaptor<SettingsEntity> captor = ArgumentCaptor.forClass(SettingsEntity.class);
        verify(settingsStorage).saveSettings(captor.capture());

        SettingsEntity savedEntity = captor.getValue();
        assertEquals("global", savedEntity.getId());
        assertEquals(60, savedEntity.getRetentionPeriodDays());
        assertEquals("api-user", savedEntity.getUpdatedBy());
        assertNotNull(savedEntity.getUpdatedAt());

        assertEquals(dto, result);
    }

    @Test
    void updateSettings_shouldNotStoreMaxExportSize() {
        // Given
        SettingsDto dto = new SettingsDto(45, 5000);

        // When
        settingsService.updateSettings(dto);

        // Then
        ArgumentCaptor<SettingsEntity> captor = ArgumentCaptor.forClass(SettingsEntity.class);
        verify(settingsStorage).saveSettings(captor.capture());

        SettingsEntity savedEntity = captor.getValue();
        assertEquals(45, savedEntity.getRetentionPeriodDays());
        // maxExportSize is not stored in entity, only retentionPeriodDays
    }
}
