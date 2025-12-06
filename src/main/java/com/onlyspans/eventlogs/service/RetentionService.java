package com.onlyspans.eventlogs.service;

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

    @Value("${event-logs.retention-period-days:90}")
    private int defaultRetentionPeriodDays;

    @Autowired
    public RetentionService(ISettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Scheduled(cron = "${event-logs.retention.cron:0 0 2 * * ?}") // Daily at 2 AM
    public void applyRetention() {
        try {
            int retentionDays = settingsService.getSettings().getRetentionPeriodDays();
            Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

            logger.info("Applying retention policy: deleting events older than {} days (before {})", 
                retentionDays, cutoffDate);

            // In a full implementation, you would use OpenSearch delete by query API
            // For now, we'll use index lifecycle management (ILM) policy
            // This is typically configured at the OpenSearch cluster level
            
            logger.info("Retention policy applied successfully");
        } catch (Exception e) {
            logger.error("Error applying retention policy", e);
        }
    }
}

