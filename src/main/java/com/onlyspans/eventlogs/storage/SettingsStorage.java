package com.onlyspans.eventlogs.storage;

import com.onlyspans.eventlogs.entity.SettingsEntity;
import com.onlyspans.eventlogs.entity.jpa.SettingsJpaEntity;
import com.onlyspans.eventlogs.repository.SettingsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

public class SettingsStorage implements ISettingsStorage {

    private static final Logger logger = LoggerFactory.getLogger(SettingsStorage.class);
    private static final String GLOBAL_ID = "global";

    private final SettingsRepository settingsRepository;

    @Autowired
    public SettingsStorage(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    @Override
    public Optional<SettingsEntity> getSettings() {
        try {
            return settingsRepository.findById(GLOBAL_ID)
                    .map(SettingsJpaEntity::toEntity);
        } catch (Exception e) {
            logger.error("Error retrieving settings from storage", e);
            return Optional.empty();
        }
    }

    @Override
    public void saveSettings(SettingsEntity settings) {
        try {
            SettingsJpaEntity jpaEntity = SettingsJpaEntity.fromEntity(settings);
            settingsRepository.save(jpaEntity);
            logger.info("Successfully saved settings to storage");
        } catch (Exception e) {
            logger.error("Error saving settings to storage", e);
            throw new RuntimeException("Failed to save settings to storage", e);
        }
    }
}
