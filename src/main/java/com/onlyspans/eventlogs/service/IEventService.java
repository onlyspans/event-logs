package com.onlyspans.eventlogs.service;

import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.dto.QueryResult;

import java.io.OutputStream;
import java.util.List;

public interface IEventService {
    void ingestEvents(List<EventDto> events);
    QueryResult searchEvents(QueryDto query);
    void exportCsv(QueryDto query, OutputStream outputStream);
}

