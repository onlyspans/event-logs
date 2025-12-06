package com.onlyspans.eventlogs.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SettingsDto {
    @NotNull(message = "Retention period days is required")
    @Min(value = 1, message = "Retention period must be at least 1 day")
    private Integer retentionPeriodDays;

    @NotNull(message = "Max export size is required")
    @Min(value = 1, message = "Max export size must be at least 1")
    private Integer maxExportSize;

    public SettingsDto() {
    }

    public SettingsDto(Integer retentionPeriodDays, Integer maxExportSize) {
        this.retentionPeriodDays = retentionPeriodDays;
        this.maxExportSize = maxExportSize;
    }

    // Getters and Setters
    public Integer getRetentionPeriodDays() {
        return retentionPeriodDays;
    }

    public void setRetentionPeriodDays(Integer retentionPeriodDays) {
        this.retentionPeriodDays = retentionPeriodDays;
    }

    public Integer getMaxExportSize() {
        return maxExportSize;
    }

    public void setMaxExportSize(Integer maxExportSize) {
        this.maxExportSize = maxExportSize;
    }
}

