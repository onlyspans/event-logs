package com.onlyspans.eventlogs.service;

import com.onlyspans.eventlogs.repository.EventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class RetentionService {

    private static final Logger logger = LoggerFactory.getLogger(RetentionService.class);

    private final ISettingsService settingsService;
    private final EventRepository eventRepository;

    @Autowired
    public RetentionService(ISettingsService settingsService, EventRepository eventRepository) {
        this.settingsService = settingsService;
        this.eventRepository = eventRepository;
    }

    @Scheduled(cron = "${event-logs.retention.cron:0 0 2 * * ?}") // Daily at 2 AM
    @Transactional
    public void applyRetention() {
        try {
            int retentionDays = settingsService.getSettings().getRetentionPeriodDays();
            Instant cutoffDate = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

            logger.info("Applying retention policy: deleting events older than {} days (before {})",
                retentionDays, cutoffDate);

            int deletedCount = eventRepository.deleteEventsOlderThan(cutoffDate);

            logger.info("Retention policy applied successfully: deleted {} events older than {}",
                deletedCount, cutoffDate);
        } catch (Exception e) {
            logger.error("Error applying retention policy", e);
        }
    }
}
