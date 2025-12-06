package com.onlyspans.eventlogs.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
public class QueryDto {
    private String user;
    private String category;
    private String action;
    private String document;
    private String project;
    private String environment;
    private String tenant;
    private String correlationId;
    private String traceId;
    private Instant startDate;
    private Instant endDate;

    private String sortBy = "timestamp";
    private String sortOrder = "desc";

    @Min(value = 0, message = "Page must be >= 0")
    private Integer page = 0;

    @Min(value = 1, message = "Size must be >= 1")
    private Integer size = 20;
}

