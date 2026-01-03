package com.onlyspans.eventlogs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyspans.eventlogs.repository.EventRepository;
import com.onlyspans.eventlogs.repository.SettingsRepository;
import com.onlyspans.eventlogs.storage.*;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfiguration {

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "postgres", matchIfMissing = true)
    public PostgresEventStorage postgresEventStorage(EventRepository eventRepository) {
        return new PostgresEventStorage(eventRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "opensearch")
    public OpenSearchEventStorage openSearchEventStorage(RestHighLevelClient client, ObjectMapper objectMapper) {
        return new OpenSearchEventStorage(client, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "postgres", matchIfMissing = true)
    public PostgresSettingsStorage postgresSettingsStorage(SettingsRepository settingsRepository) {
        return new PostgresSettingsStorage(settingsRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "opensearch")
    public OpenSearchSettingsStorage openSearchSettingsStorage(RestHighLevelClient client, ObjectMapper objectMapper) {
        return new OpenSearchSettingsStorage(client, objectMapper);
    }
}
