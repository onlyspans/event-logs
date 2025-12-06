package com.onlyspans.eventlogs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EventDto {

    @NotBlank(message = "ID is required")
    private String id;

    @NotNull(message = "Timestamp is required")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @NotBlank(message = "User is required")
    private String user;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Action is required")
    private String action;

    @NotBlank(message = "Document is required")
    private String document;

    @NotBlank(message = "Project is required")
    private String project;

    @NotBlank(message = "Environment is required")
    private String environment;

    @NotBlank(message = "Tenant is required")
    private String tenant;

    @Valid
    private EventDetailsDto details;

    @Data
    @NoArgsConstructor
    public static class EventDetailsDto {
        private List<ChangeDto> changes;
        private String ipAddress;
        private String userAgent;
        private String additionalInfo;
    }

    @Data
    @NoArgsConstructor
    public static class ChangeDto {
        private String field;
        private String oldValue;
        private String newValue;
    }
}
