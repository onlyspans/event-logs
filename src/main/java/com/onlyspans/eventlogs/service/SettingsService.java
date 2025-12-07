package com.onlyspans.eventlogs.service;

import com.onlyspans.eventlogs.dto.SettingsDto;
import com.onlyspans.eventlogs.entity.SettingsEntity;
import com.onlyspans.eventlogs.storage.ISettingsStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class SettingsService implements ISettingsService {

    private static final Logger logger = LoggerFactory.getLogger(SettingsService.class);

    @Value("${event-logs.retention-period-days:90}")
    private int defaultRetentionPeriodDays;

    @Value("${event-logs.max-export-size:10000}")
    private int defaultMaxExportSize;

    private final ISettingsStorage settingsStorage;

    @Autowired
    public SettingsService(ISettingsStorage settingsStorage) {
        this.settingsStorage = settingsStorage;
    }

    @Override
    public SettingsDto getSettings() {
        Integer retentionPeriodDays = settingsStorage.getSettings()
                .map(SettingsEntity::getRetentionPeriodDays)
                .orElse(defaultRetentionPeriodDays);

        return new SettingsDto(retentionPeriodDays, defaultMaxExportSize);
    }

    @Override
    public SettingsDto updateSettings(SettingsDto settings) {
        logger.info("Updating settings: retentionPeriodDays={}, maxExportSize={}",
                settings.getRetentionPeriodDays(), settings.getMaxExportSize());

        // Only retention period is stored in OpenSearch
        // maxExportSize is application-level configuration only
        SettingsEntity entity = new SettingsEntity(
                "global",
                settings.getRetentionPeriodDays(),
                Instant.now(),
                "api-user"
        );

        settingsStorage.saveSettings(entity);

        return settings;
    }
}

