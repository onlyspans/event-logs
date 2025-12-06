package com.onlyspans.eventlogs.config;

import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.CreateIndexRequest;
import org.opensearch.client.indices.GetIndexRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class OpenSearchIndexConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchIndexConfig.class);
    private static final String INDEX_NAME = "event-logs";

    private final RestHighLevelClient client;

    @Autowired
    public OpenSearchIndexConfig(RestHighLevelClient client) {
        this.client = client;
    }

    @Override
    public void run(String... args) {
        createIndexIfNotExists();
    }

    private void createIndexIfNotExists() {
        try {
            GetIndexRequest getIndexRequest = new GetIndexRequest(INDEX_NAME);
            boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
            if (!exists) {
                logger.info("Creating OpenSearch index: {}", INDEX_NAME);
                CreateIndexRequest createIndexRequest = new CreateIndexRequest(INDEX_NAME);
                client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
                logger.info("Successfully created OpenSearch index: {}", INDEX_NAME);
            } else {
                logger.debug("OpenSearch index already exists: {}", INDEX_NAME);
            }
        } catch (Exception e) {
            logger.error("Error creating OpenSearch index", e);
            // Do not fail startup if index creation fails
        }
    }
}

