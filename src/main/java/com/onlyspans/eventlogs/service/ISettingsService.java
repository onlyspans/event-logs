package com.onlyspans.eventlogs.service;

import com.onlyspans.eventlogs.dto.SettingsDto;

public interface ISettingsService {
    SettingsDto getSettings();
    SettingsDto updateSettings(SettingsDto settings);
}

