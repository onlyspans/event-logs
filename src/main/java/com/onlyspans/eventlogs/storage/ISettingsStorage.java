package com.onlyspans.eventlogs.storage;

import com.onlyspans.eventlogs.entity.SettingsEntity;

import java.util.Optional;

public interface ISettingsStorage {

    Optional<SettingsEntity> getSettings();

    void saveSettings(SettingsEntity settings);
}
