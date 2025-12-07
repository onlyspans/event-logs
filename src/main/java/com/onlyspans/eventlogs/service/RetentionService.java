package com.onlyspans.eventlogs.service;

import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.reindex.BulkByScrollResponse;
import org.opensearch.index.reindex.DeleteByQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RetentionService {

    private static final Logger logger = LoggerFactory.getLogger(RetentionService.class);
    private static final String INDEX_NAME = "event-logs";

    private final ISettingsService settingsService;
    private final RestHighLevelClient client;

    @Value("${event-logs.retention-period-days:90}")
    private int defaultRetentionPeriodDays;

    @Autowired
    public RetentionService(ISettingsService settingsService, RestHighLevelClient client) {
        this.settingsService = settingsService;
        this.client = client;
    }

    @Scheduled(cron = "${event-logs.retention.cron:0 0 2 * * ?}") // Daily at 2 AM
    public void applyRetention() {
        try {
            int retentionDays = settingsService.getSettings().getRetentionPeriodDays();
            Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

            logger.info("Applying retention policy: deleting events older than {} days (before {})",
                retentionDays, cutoffDate);

            DeleteByQueryRequest request = new DeleteByQueryRequest(INDEX_NAME);
            request.setQuery(QueryBuilders.rangeQuery("timestamp").lt(cutoffDate.toEpochMilli()));
            request.setRefresh(true);
            request.setBatchSize(1000);
            request.setSlices(2);

            BulkByScrollResponse response = client.deleteByQuery(request, RequestOptions.DEFAULT);
            long deletedCount = response.getDeleted();

            logger.info("Retention policy applied successfully: deleted {} documents older than {}",
                deletedCount, cutoffDate);
        } catch (Exception e) {
            logger.error("Error applying retention policy", e);
        }
    }
}

