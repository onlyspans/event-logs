package com.onlyspans.eventlogs.config;

import com.onlyspans.eventlogs.repository.EventRepository;
import com.onlyspans.eventlogs.repository.SettingsRepository;
import com.onlyspans.eventlogs.storage.EventStorage;
import com.onlyspans.eventlogs.storage.SettingsStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    public EventStorage eventStorage(EventRepository eventRepository) {
        return new EventStorage(eventRepository);
    }

    @Bean
    public SettingsStorage settingsStorage(SettingsRepository settingsRepository) {
        return new SettingsStorage(settingsRepository);
    }
}
