package com.onlyspans.eventlogs.service;

import com.onlyspans.eventlogs.dto.SettingsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SettingsService implements ISettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);
    private static final String SETTINGS_INDEX = ".event-logs-settings";

    @Value("${event-logs.retention-period-days:90}")
    private int defaultRetentionPeriodDays;

    @Value("${event-logs.max-export-size:10000}")
    private int defaultMaxExportSize;

    @Autowired
    public SettingsService() {
    }

    @Override
    public SettingsDto getSettings() {
        // For now, return default settings from configuration
        // In a full implementation, you would read from OpenSearch index
        return new SettingsDto(defaultRetentionPeriodDays, defaultMaxExportSize);
    }

    @Override
    public SettingsDto updateSettings(SettingsDto settings) {
        // Validate settings
        if (settings.getRetentionPeriodDays() < 1) {
            throw new IllegalArgumentException("Retention period must be at least 1 day");
        }
        if (settings.getMaxExportSize() < 1) {
            throw new IllegalArgumentException("Max export size must be at least 1");
        }

        // In a full implementation, you would save to OpenSearch index
        // For now, we'll just log and return
        logger.info("Updating settings: retentionPeriodDays={}, maxExportSize={}",
            settings.getRetentionPeriodDays(), settings.getMaxExportSize());

        return settings;
    }
}

