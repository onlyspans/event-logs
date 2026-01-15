package com.onlyspans.eventlogs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class ExportEventsRequest {
    @JsonProperty("user")
    private String user;

    @JsonProperty("category")
    private String category;

    @JsonProperty("action")
    private String action;

    @JsonProperty("document")
    private String document;

    @JsonProperty("project")
    private String project;

    @JsonProperty("environment")
    private String environment;

    @JsonProperty("tenant")
    private String tenant;

    @JsonProperty("correlationId")
    private String correlationId;

    @JsonProperty("traceId")
    private String traceId;

    @JsonProperty("startDate")
    private Instant startDate;

    @JsonProperty("endDate")
    private Instant endDate;

    @JsonProperty("sortBy")
    private String sortBy = "timestamp";

    @JsonProperty("sortOrder")
    private String sortOrder = "desc";
}
