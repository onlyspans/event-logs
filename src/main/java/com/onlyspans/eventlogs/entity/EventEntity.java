package com.onlyspans.eventlogs.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
public class EventEntity {

    private String id;
    private Instant timestamp;
    private String user;
    private String category;
    private String action;

    @JsonProperty("document")
    private String documentName;

    private String project;
    private String environment;
    private String tenant;
    private String correlationId;
    private String traceId;
    private EventDetails details;

    @Data
    public static class EventDetails {
        private List<Change> changes;
        private String ipAddress;
        private String userAgent;
        private String additionalInfo;
    }

    @Data
    public static class Change {
        private String field;
        private String oldValue;
        private String newValue;
    }
}

