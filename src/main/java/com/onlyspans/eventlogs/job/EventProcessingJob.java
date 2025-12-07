package com.onlyspans.eventlogs.job;

import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.input.IEventInput;
import com.onlyspans.eventlogs.storage.IEventStorage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public final class EventProcessingJob {

    private static final Logger logger = LoggerFactory.getLogger(EventProcessingJob.class);

    private final IEventInput eventInput;
    private final IEventStorage eventStorage;

    private final Counter eventsProcessedCounter;
    private final Counter jobExecutionCounter;

    @Autowired
    public EventProcessingJob(
            IEventInput eventInput,
            IEventStorage eventStorage,
            MeterRegistry meterRegistry
    ) {
        this.eventInput = eventInput;
        this.eventStorage = eventStorage;

        this.eventsProcessedCounter = Counter.builder("event_logs.job.processed")
                .description("Total number of events processed by job")
                .register(meterRegistry);

        this.jobExecutionCounter = Counter.builder("event_logs.job.executions")
                .description("Total number of job executions")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${event-logs.job.interval-ms:5000}")
    public void execute() {
        // TODO: rework, read batches all the time
        try {
            jobExecutionCounter.increment();
            logger.debug("Starting event processing job");

            List<EventEntity> events = eventInput.read();

            if (events.isEmpty()) {
                logger.debug("No events to process");
                return;
            }

            logger.info("Processing {} events from Kafka", events.size());
            eventStorage.add(events);
            eventsProcessedCounter.increment(events.size());

            logger.info("Successfully processed {} events", events.size());
        } catch (Exception e) {
            logger.error("Error in event processing job", e);
            // Don't throw exception to prevent job from stopping
        }
    }
}

