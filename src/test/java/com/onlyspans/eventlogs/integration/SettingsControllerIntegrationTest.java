package com.onlyspans.eventlogs.integration;

import com.onlyspans.eventlogs.dto.SettingsDto;
import com.onlyspans.eventlogs.entity.SettingsEntity;
import com.onlyspans.eventlogs.repository.SettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SettingsControllerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private SettingsRepository settingsRepository;

    private RestTemplate restTemplate = new RestTemplate();

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @AfterEach
    void tearDown() {
        settingsRepository.deleteAll();
    }

    @Test
    void getSettings_shouldReturnDefaultsWhenNoSettingsExist() {
        // When
        ResponseEntity<SettingsDto> response = restTemplate.getForEntity(
                getBaseUrl() + "/settings",
                SettingsDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(90, response.getBody().getRetentionPeriodDays());
        assertEquals(10000, response.getBody().getMaxExportSize());
    }

    @Test
    void getSettings_shouldReturnStoredSettings() {
        // Given
        SettingsEntity settings = new SettingsEntity(
                "global",
                45,
                Instant.now(),
                "test-user"
        );
        settingsRepository.save(settings);

        // When
        ResponseEntity<SettingsDto> response = restTemplate.getForEntity(
                getBaseUrl() + "/settings",
                SettingsDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(45, response.getBody().getRetentionPeriodDays());
        assertEquals(10000, response.getBody().getMaxExportSize());
    }

    @Test
    void updateSettings_shouldSaveNewSettings() {
        // Given
        SettingsDto requestDto = new SettingsDto(60, 10000);

        // When
        ResponseEntity<SettingsDto> response = restTemplate.exchange(
                getBaseUrl() + "/settings",
                HttpMethod.PUT,
                new HttpEntity<>(requestDto),
                SettingsDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(60, response.getBody().getRetentionPeriodDays());

        // Verify in database
        SettingsEntity saved = settingsRepository.findById("global").orElse(null);
        assertNotNull(saved);
        assertEquals(60, saved.getRetentionPeriodDays());
        assertEquals("api-user", saved.getUpdatedBy());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void updateSettings_shouldUpdateExistingSettings() {
        // Given - existing settings
        SettingsEntity existing = new SettingsEntity(
                "global",
                30,
                Instant.now().minusSeconds(3600),
                "old-user"
        );
        settingsRepository.save(existing);

        // When - update
        SettingsDto requestDto = new SettingsDto(90, 10000);
        ResponseEntity<SettingsDto> response = restTemplate.exchange(
                getBaseUrl() + "/settings",
                HttpMethod.PUT,
                new HttpEntity<>(requestDto),
                SettingsDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(90, response.getBody().getRetentionPeriodDays());

        // Verify in database
        SettingsEntity updated = settingsRepository.findById("global").orElse(null);
        assertNotNull(updated);
        assertEquals(90, updated.getRetentionPeriodDays());
        assertEquals("api-user", updated.getUpdatedBy());
        assertTrue(updated.getUpdatedAt().isAfter(existing.getUpdatedAt()));
    }

    @Test
    void updateSettings_shouldValidateRetentionPeriod() {
        // Given - invalid retention period (0)
        SettingsDto invalidDto = new SettingsDto(0, 10000);

        // When
        ResponseEntity<SettingsDto> response = restTemplate.exchange(
                getBaseUrl() + "/settings",
                HttpMethod.PUT,
                new HttpEntity<>(invalidDto),
                SettingsDto.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateSettings_shouldValidateRetentionPeriodMaxValue() {
        // Given - invalid retention period (too large)
        SettingsDto invalidDto = new SettingsDto(3651, 10000);

        // When
        ResponseEntity<SettingsDto> response = restTemplate.exchange(
                getBaseUrl() + "/settings",
                HttpMethod.PUT,
                new HttpEntity<>(invalidDto),
                SettingsDto.class
        );

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void updateSettings_shouldAcceptValidRetentionPeriodRange() {
        // Given - valid retention periods
        SettingsDto dto1 = new SettingsDto(1, 10000);
        SettingsDto dto2 = new SettingsDto(3650, 10000);

        // When
        ResponseEntity<SettingsDto> response1 = restTemplate.exchange(
                getBaseUrl() + "/settings",
                HttpMethod.PUT,
                new HttpEntity<>(dto1),
                SettingsDto.class
        );

        ResponseEntity<SettingsDto> response2 = restTemplate.exchange(
                getBaseUrl() + "/settings",
                HttpMethod.PUT,
                new HttpEntity<>(dto2),
                SettingsDto.class
        );

        // Then
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
    }
}
