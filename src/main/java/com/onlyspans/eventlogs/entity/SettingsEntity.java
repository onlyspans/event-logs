package com.onlyspans.eventlogs.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "settings")
public class SettingsEntity {

    @Id
    @JsonProperty("id")
    private String id;

    @JsonProperty("retentionPeriodDays")
    @Column(name = "retention_period_days")
    private Integer retentionPeriodDays;

    @JsonProperty("updatedAt")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @JsonProperty("updatedBy")
    @Column(name = "updated_by")
    private String updatedBy;

    public SettingsEntity(Integer retentionPeriodDays) {
        this.id = "global";
        this.retentionPeriodDays = retentionPeriodDays;
        this.updatedAt = Instant.now();
        this.updatedBy = "system";
    }
}
