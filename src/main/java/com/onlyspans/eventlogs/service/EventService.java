package com.onlyspans.eventlogs.service;

import com.opencsv.CSVWriter;
import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.dto.QueryResult;
import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.storage.IEventStorage;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Validated
public class EventService implements IEventService {

    private static final Logger logger = LoggerFactory.getLogger(EventService.class);

    private final IEventStorage eventStorage;
    private final Counter eventsIngestedCounter;
    private final Counter eventsSearchedCounter;
    private final Counter eventsExportedCounter;

    @Value("${event-logs.max-export-size:10000}")
    private int maxExportSize;

    @Autowired
    public EventService(IEventStorage eventStorage, MeterRegistry meterRegistry) {
        this.eventStorage = eventStorage;
        this.eventsIngestedCounter = Counter.builder("event_logs.ingested")
            .description("Total number of events ingested")
            .register(meterRegistry);
        this.eventsSearchedCounter = Counter.builder("event_logs.searched")
            .description("Total number of search operations")
            .register(meterRegistry);
        this.eventsExportedCounter = Counter.builder("event_logs.exported")
            .description("Total number of events exported")
            .register(meterRegistry);
    }

    @Override
    public void ingestEvents(@Valid List<EventDto> events) {
        if (events == null || events.isEmpty()) {
            logger.warn("Attempted to ingest empty or null event list");
            return;
        }

        try {
            List<EventEntity> entities = events.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());

            eventStorage.add(entities);
            eventsIngestedCounter.increment(events.size());
            logger.info("Successfully ingested {} events", events.size());
        } catch (Exception e) {
            logger.error("Error ingesting events", e);
            throw new RuntimeException("Failed to ingest events", e);
        }
    }

    @Override
    public QueryResult searchEvents(QueryDto query) {
        try {
            eventsSearchedCounter.increment();
            
            List<EventEntity> entities = eventStorage.search(query);
            long total = eventStorage.count(query);

            List<EventDto> dtos = entities.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

            return new QueryResult(dtos, total, query.getPage(), query.getSize());
        } catch (Exception e) {
            logger.error("Error searching events", e);
            throw new RuntimeException("Failed to search events", e);
        }
    }

    @Override
    public void exportCsv(QueryDto query, OutputStream outputStream) {
        try {
            // Limit export size
            QueryDto limitedQuery = new QueryDto();
            limitedQuery.setUser(query.getUser());
            limitedQuery.setCategory(query.getCategory());
            limitedQuery.setAction(query.getAction());
            limitedQuery.setDocument(query.getDocument());
            limitedQuery.setProject(query.getProject());
            limitedQuery.setEnvironment(query.getEnvironment());
            limitedQuery.setTenant(query.getTenant());
            limitedQuery.setCorrelationId(query.getCorrelationId());
            limitedQuery.setTraceId(query.getTraceId());
            limitedQuery.setStartDate(query.getStartDate());
            limitedQuery.setEndDate(query.getEndDate());
            limitedQuery.setSortBy(query.getSortBy());
            limitedQuery.setSortOrder(query.getSortOrder());
            limitedQuery.setPage(0);
            limitedQuery.setSize(maxExportSize);

            List<EventEntity> entities = eventStorage.search(limitedQuery);
            
            if (entities.size() >= maxExportSize) {
                logger.warn("Export result size ({}) reached max export size limit ({})", 
                    entities.size(), maxExportSize);
            }

            try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
                
                // Write header
                writer.writeNext(new String[]{
                    "ID", "Timestamp", "User", "Category", "Action", "Document",
                    "Project", "Environment", "Tenant", "Correlation ID", "Trace ID",
                    "IP Address", "User Agent", "Additional Info"
                });

                // Write data
                for (EventEntity entity : entities) {
                    writer.writeNext(new String[]{
                        entity.getId(),
                        entity.getTimestamp() != null ? entity.getTimestamp().toString() : "",
                        entity.getUser(),
                        entity.getCategory(),
                        entity.getAction(),
                        entity.getDocument(),
                        entity.getProject(),
                        entity.getEnvironment(),
                        entity.getTenant(),
                        entity.getCorrelationId() != null ? entity.getCorrelationId() : "",
                        entity.getTraceId() != null ? entity.getTraceId() : "",
                        entity.getDetails() != null && entity.getDetails().getIpAddress() != null 
                            ? entity.getDetails().getIpAddress() : "",
                        entity.getDetails() != null && entity.getDetails().getUserAgent() != null 
                            ? entity.getDetails().getUserAgent() : "",
                        entity.getDetails() != null && entity.getDetails().getAdditionalInfo() != null 
                            ? entity.getDetails().getAdditionalInfo() : ""
                    });
                }
            }

            eventsExportedCounter.increment(entities.size());
            logger.info("Successfully exported {} events to CSV", entities.size());
        } catch (IOException e) {
            logger.error("Error exporting events to CSV", e);
            throw new RuntimeException("Failed to export events to CSV", e);
        }
    }

    private EventEntity convertToEntity(EventDto dto) {
        EventEntity entity = new EventEntity();
        entity.setId(dto.getId());
        entity.setTimestamp(dto.getTimestamp() != null ? dto.getTimestamp() : Instant.now());
        entity.setUser(dto.getUser());
        entity.setCategory(dto.getCategory());
        entity.setAction(dto.getAction());
        entity.setDocument(dto.getDocument());
        entity.setProject(dto.getProject());
        entity.setEnvironment(dto.getEnvironment());
        entity.setTenant(dto.getTenant());

        if (dto.getDetails() != null) {
            EventEntity.EventDetails details = new EventEntity.EventDetails();
            details.setIpAddress(dto.getDetails().getIpAddress());
            details.setUserAgent(dto.getDetails().getUserAgent());
            details.setAdditionalInfo(dto.getDetails().getAdditionalInfo());

            if (dto.getDetails().getChanges() != null) {
                List<EventEntity.Change> changes = dto.getDetails().getChanges().stream()
                    .map(changeDto -> {
                        EventEntity.Change change = new EventEntity.Change();
                        change.setField(changeDto.getField());
                        change.setOldValue(changeDto.getOldValue());
                        change.setNewValue(changeDto.getNewValue());
                        return change;
                    })
                    .toList();
                details.setChanges(changes);
            }

            entity.setDetails(details);
        }

        return entity;
    }

    private EventDto convertToDto(EventEntity entity) {
        EventDto dto = new EventDto();
        dto.setId(entity.getId());
        dto.setTimestamp(entity.getTimestamp());
        dto.setUser(entity.getUser());
        dto.setCategory(entity.getCategory());
        dto.setAction(entity.getAction());
        dto.setDocument(entity.getDocument());
        dto.setProject(entity.getProject());
        dto.setEnvironment(entity.getEnvironment());
        dto.setTenant(entity.getTenant());

        if (entity.getDetails() != null) {
            EventDto.EventDetailsDto detailsDto = new EventDto.EventDetailsDto();
            detailsDto.setIpAddress(entity.getDetails().getIpAddress());
            detailsDto.setUserAgent(entity.getDetails().getUserAgent());
            detailsDto.setAdditionalInfo(entity.getDetails().getAdditionalInfo());

            if (entity.getDetails().getChanges() != null) {
                List<EventDto.ChangeDto> changes = entity.getDetails().getChanges().stream()
                    .map(change -> {
                        EventDto.ChangeDto changeDto = new EventDto.ChangeDto();
                        changeDto.setField(change.getField());
                        changeDto.setOldValue(change.getOldValue());
                        changeDto.setNewValue(change.getNewValue());
                        return changeDto;
                    })
                    .toList();
                detailsDto.setChanges(changes);
            }

            dto.setDetails(detailsDto);
        }

        return dto;
    }
}

