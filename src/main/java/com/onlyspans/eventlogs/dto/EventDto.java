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
    @JsonProperty("id")
    private String id;

    @NotNull(message = "Timestamp is required")
    @JsonProperty("timestamp")
    private Instant timestamp;

    @NotBlank(message = "User is required")
    @JsonProperty("user")
    private String user;

    @NotBlank(message = "Category is required")
    @JsonProperty("category")
    private String category;

    @NotBlank(message = "Action is required")
    @JsonProperty("action")
    private String action;

    @NotBlank(message = "Document is required")
    @JsonProperty("document")
    private String document;

    @NotBlank(message = "Project is required")
    @JsonProperty("project")
    private String project;

    @NotBlank(message = "Environment is required")
    @JsonProperty("environment")
    private String environment;

    @NotBlank(message = "Tenant is required")
    @JsonProperty("tenant")
    private String tenant;

    @Valid
    @JsonProperty("details")
    private EventDetailsDto details;

    @Data
    @NoArgsConstructor
    public static class EventDetailsDto {
        @JsonProperty("changes")
        private List<ChangeDto> changes;

        @JsonProperty("ipAddress")
        private String ipAddress;

        @JsonProperty("userAgent")
        private String userAgent;

        @JsonProperty("additionalInfo")
        private String additionalInfo;
    }

    @Data
    @NoArgsConstructor
    public static class ChangeDto {
        @JsonProperty("field")
        private String field;

        @JsonProperty("oldValue")
        private String oldValue;

        @JsonProperty("newValue")
        private String newValue;
    }
}
