package com.onlyspans.eventlogs.controller;

import com.onlyspans.eventlogs.dto.ExportEventsRequest;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.dto.QueryResult;
import com.onlyspans.eventlogs.dto.SearchEventsRequest;
import com.onlyspans.eventlogs.mapper.EventMapper;
import com.onlyspans.eventlogs.service.IEventService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
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
    private final EventMapper eventMapper;

    @Autowired
    public EventController(IEventService eventService, EventMapper eventMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
    }

    @PostMapping
    public QueryResult searchEvents(@Valid @RequestBody SearchEventsRequest request) {
        QueryDto query = eventMapper.toQueryDto(request);
        logger.debug("Searching events with query: {}", query);
        return eventService.searchEvents(query);
    }

    @PostMapping("/export")
    public void exportEvents(@Valid @RequestBody ExportEventsRequest request, HttpServletResponse response) throws Exception {
        QueryDto query = eventMapper.toQueryDto(request);

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

