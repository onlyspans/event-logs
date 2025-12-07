package com.onlyspans.eventlogs.controller;

import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.dto.QueryResult;
import com.onlyspans.eventlogs.service.IEventService;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/events")
public final class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    private final IEventService eventService;

    @Autowired
    public EventController(IEventService eventService) {
        this.eventService = eventService;
    }

    // TODO: maybe there's a way to represent query parameters as an object.
    // TODO: if the thing above is possible, extract QueryDto building into method (now IDE tells it's duplicate code)
    @GetMapping
    public QueryResult searchEvents(
        @RequestParam(required = false) String user,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String document,
        @RequestParam(required = false) String project,
        @RequestParam(required = false) String environment,
        @RequestParam(required = false) String tenant,
        @RequestParam(required = false) String correlationId,
        @RequestParam(required = false) String traceId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        @RequestParam(required = false, defaultValue = "timestamp") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortOrder,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        QueryDto query = new QueryDto();
        query.setUser(user);
        query.setCategory(category);
        query.setAction(action);
        query.setDocument(document);
        query.setProject(project);
        query.setEnvironment(environment);
        query.setTenant(tenant);
        query.setCorrelationId(correlationId);
        query.setTraceId(traceId);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setSortBy(sortBy);
        query.setSortOrder(sortOrder);
        query.setPage(page);
        query.setSize(size);

        logger.debug("Searching events with query: {}", query);
        return eventService.searchEvents(query);
    }

    @GetMapping("/export")
    public void exportEvents(
        @RequestParam(required = false) String user,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String document,
        @RequestParam(required = false) String project,
        @RequestParam(required = false) String environment,
        @RequestParam(required = false) String tenant,
        @RequestParam(required = false) String correlationId,
        @RequestParam(required = false) String traceId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
        @RequestParam(required = false, defaultValue = "timestamp") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortOrder,
        HttpServletResponse response
    ) throws Exception {
        QueryDto query = new QueryDto();
        query.setUser(user);
        query.setCategory(category);
        query.setAction(action);
        query.setDocument(document);
        query.setProject(project);
        query.setEnvironment(environment);
        query.setTenant(tenant);
        query.setCorrelationId(correlationId);
        query.setTraceId(traceId);
        query.setStartDate(startDate);
        query.setEndDate(endDate);
        query.setSortBy(sortBy);
        query.setSortOrder(sortOrder);
        query.setPage(0);
        query.setSize(Integer.MAX_VALUE); // Will be limited by service

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        String filename = String.format("events-export_%s_utc.csv", timestamp);

        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"" + filename + "\"");

        eventService.exportCsv(query, response.getOutputStream());
    }
}

