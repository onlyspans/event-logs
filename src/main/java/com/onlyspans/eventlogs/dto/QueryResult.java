package com.onlyspans.eventlogs.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {
    private List<EventDto> events;
    private long total;
    private int page;
    private int size;
}

