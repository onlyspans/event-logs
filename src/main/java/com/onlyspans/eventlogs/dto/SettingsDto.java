package com.onlyspans.eventlogs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsDto {

    @NotNull(message = "Retention period days is required")
    @Min(value = 1, message = "Retention period must be at least 1 day")
    @Max(value = 3650, message = "Retention period must be at most 3650 days (10 years)")
    @JsonProperty("retentionPeriodDays")
    private Integer retentionPeriodDays;

    @NotNull(message = "Max export size is required")
    @Min(value = 1, message = "Max export size must be at least 1")
    @JsonProperty("maxExportSize")
    private Integer maxExportSize;
}