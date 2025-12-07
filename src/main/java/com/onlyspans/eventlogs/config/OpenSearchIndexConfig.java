package com.onlyspans.eventlogs.config;

import org.jetbrains.annotations.NotNull;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public final class OpenSearchIndexConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchIndexConfig.class);
    private static final String EVENT_LOGS_INDEX = "event-logs";
    private static final String SETTINGS_INDEX = ".event-logs-settings";

    private final RestHighLevelClient client;

    @Autowired
    public OpenSearchIndexConfig(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public void run(String @NotNull ... args) throws IOException {
        createIndexIfNotExists(EVENT_LOGS_INDEX);
        createIndexIfNotExists(SETTINGS_INDEX);
    }

    private void createIndexIfNotExists(String indexName) throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest(indexName);
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if (!exists) {
            logger.info("Creating OpenSearch index: {}", indexName);
            CreateIndexRequest createIndexRequest = new CreateIndexRequest(indexName);
            client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
            logger.info("Successfully created OpenSearch index: {}", indexName);
        } else {
            logger.debug("OpenSearch index already exists: {}", indexName);
        }
    }
}

