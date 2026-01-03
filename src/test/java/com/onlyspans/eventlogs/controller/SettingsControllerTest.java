package com.onlyspans.eventlogs.controller;

import com.onlyspans.eventlogs.dto.SettingsDto;
import com.onlyspans.eventlogs.service.ISettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettingsControllerTest {

    @Mock
    private ISettingsService settingsService;

    private SettingsController settingsController;

    @BeforeEach
    void setUp() {
        settingsController = new SettingsController(settingsService);
    }

    @Test
    void getSettings_shouldReturnSettings() {
        // Given
        SettingsDto settings = new SettingsDto(30, 10000);
        when(settingsService.getSettings()).thenReturn(settings);

        // When
        SettingsDto result = settingsController.getSettings();

        // Then
        assertNotNull(result);
        assertEquals(30, result.getRetentionPeriodDays());
        assertEquals(10000, result.getMaxExportSize());
        verify(settingsService).getSettings();
    }

    @Test
    void updateSettings_shouldUpdateAndReturnSettings() {
        // Given
        SettingsDto requestDto = new SettingsDto(60, 10000);
        SettingsDto responseDto = new SettingsDto(60, 10000);
        when(settingsService.updateSettings(any(SettingsDto.class))).thenReturn(responseDto);

        // When
        SettingsDto result = settingsController.updateSettings(requestDto);

        // Then
        assertNotNull(result);
        assertEquals(60, result.getRetentionPeriodDays());
        assertEquals(10000, result.getMaxExportSize());
        verify(settingsService).updateSettings(requestDto);
    }

    @Test
    void updateSettings_shouldPassDtoToService() {
        // Given
        SettingsDto dto = new SettingsDto(45, 5000);
        when(settingsService.updateSettings(any(SettingsDto.class))).thenReturn(dto);

        // When
        settingsController.updateSettings(dto);

        // Then
        verify(settingsService).updateSettings(dto);
    }
}
