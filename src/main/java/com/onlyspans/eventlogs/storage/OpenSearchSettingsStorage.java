package com.onlyspans.eventlogs.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyspans.eventlogs.entity.SettingsEntity;
import com.onlyspans.eventlogs.exception.EventStorageException;
import org.jetbrains.annotations.NotNull;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.index.IndexResponse;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

public class OpenSearchSettingsStorage implements ISettingsStorage {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchSettingsStorage.class);
    private static final String INDEX_NAME = ".event-logs-settings";
    private static final String SETTINGS_ID = "global";

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenSearchSettingsStorage(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<SettingsEntity> getSettings() {
        try {
            GetRequest getRequest = new GetRequest(INDEX_NAME, SETTINGS_ID);
            GetResponse response = client.get(getRequest, RequestOptions.DEFAULT);

            if (!response.isExists()) {
                logger.debug("Settings not found in OpenSearch, returning empty");
                return Optional.empty();
            }

            Map<String, Object> source = response.getSourceAsMap();
            SettingsEntity settings = objectMapper.convertValue(source, SettingsEntity.class);
            logger.debug("Retrieved settings from OpenSearch: {}", settings);
            return Optional.of(settings);

        } catch (Exception e) {
            logger.error("Error retrieving settings from OpenSearch", e);
            throw new EventStorageException("Failed to retrieve settings from OpenSearch", e);
        }
    }

    @Override
    public void saveSettings(@NotNull SettingsEntity settings) {
        try {
            settings.setId(SETTINGS_ID);

            @SuppressWarnings("unchecked")
            Map<String, Object> source = objectMapper.convertValue(settings, Map.class);

            IndexRequest indexRequest = new IndexRequest(INDEX_NAME)
                    .id(SETTINGS_ID)
                    .source(source)
                    .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);

            IndexResponse response = client.index(indexRequest, RequestOptions.DEFAULT);

            logger.info("Successfully saved settings to OpenSearch: {}", response.getResult());

        } catch (Exception e) {
            logger.error("Error saving settings to OpenSearch", e);
            throw new EventStorageException("Failed to save settings to OpenSearch", e);
        }
    }
}
