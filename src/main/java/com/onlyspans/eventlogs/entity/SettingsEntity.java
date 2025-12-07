package com.onlyspans.eventlogs.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsEntity {

    @JsonProperty("id")
    private String id;

    @JsonProperty("retentionPeriodDays")
    private Integer retentionPeriodDays;

    @JsonProperty("updatedAt")
    private Instant updatedAt;

    @JsonProperty("updatedBy")
    private String updatedBy;

    public SettingsEntity(Integer retentionPeriodDays) {
        this.id = "global";
        this.retentionPeriodDays = retentionPeriodDays;
        this.updatedAt = Instant.now();
        this.updatedBy = "system";
    }
}
