package com.onlyspans.eventlogs.dto;

import java.util.List;

public class QueryResult {
    private List<EventDto> events;
    private long total;
    private int page;
    private int size;

    public QueryResult() {
    }

    public QueryResult(List<EventDto> events, long total, int page, int size) {
        this.events = events;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    // Getters and Setters
    public List<EventDto> getEvents() {
        return events;
    }

    public void setEvents(List<EventDto> events) {
        this.events = events;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}

