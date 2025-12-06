package com.onlyspans.eventlogs.dto;

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
    private Integer retentionPeriodDays;

    @NotNull(message = "Max export size is required")
    @Min(value = 1, message = "Max export size must be at least 1")
    private Integer maxExportSize;
}