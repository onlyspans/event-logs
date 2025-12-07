package com.onlyspans.eventlogs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class QueryDto {
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

    @Min(value = 0, message = "Page must be >= 0")
    @JsonProperty("page")
    private Integer page = 0;

    @Min(value = 1, message = "Size must be >= 1")
    @JsonProperty("size")
    private Integer size = 20;
}

