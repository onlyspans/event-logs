package com.onlyspans.eventlogs.entity;

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
    private String id;

    @Column(name = "retention_period_days")
    private Integer retentionPeriodDays;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "updated_by")
    private String updatedBy;

    public SettingsEntity(Integer retentionPeriodDays) {
        this.id = "global";
        this.retentionPeriodDays = retentionPeriodDays;
        this.updatedAt = Instant.now();
        this.updatedBy = "system";
    }
}
