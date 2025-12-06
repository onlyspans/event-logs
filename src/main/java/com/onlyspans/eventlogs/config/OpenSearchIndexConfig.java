package com.onlyspans.eventlogs.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

import com.onlyspans.eventlogs.entity.EventEntity;

@Component
public class OpenSearchIndexConfig implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchIndexConfig.class);
    private static final String INDEX_NAME = "event-logs";

    private final ElasticsearchOperations elasticsearchOperations;

    @Value("${event-logs.retention-period-days:90}")
    private int retentionPeriodDays;

    @Autowired
    public OpenSearchIndexConfig(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public void run(String... args) throws Exception {
        createIndexIfNotExists();
    }

    private void createIndexIfNotExists() {
        try {
            IndexOperations indexOperations = elasticsearchOperations.indexOps(EventEntity.class);
            
            if (!indexOperations.exists()) {
                logger.info("Creating OpenSearch index: {}", INDEX_NAME);
                indexOperations.create();
                logger.info("Successfully created OpenSearch index: {}", INDEX_NAME);
            } else {
                logger.debug("OpenSearch index already exists: {}", INDEX_NAME);
            }
        } catch (Exception e) {
            logger.error("Error creating OpenSearch index", e);
            // Don't fail startup if index creation fails - it might already exist
        }
    }
}

