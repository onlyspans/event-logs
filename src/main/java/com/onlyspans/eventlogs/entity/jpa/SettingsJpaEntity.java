package com.onlyspans.eventlogs.entity.jpa;

import com.onlyspans.eventlogs.entity.SettingsEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsJpaEntity {

    @Id
    private String id;

    @Column(name = "retention_period_days", nullable = false)
    private Integer retentionPeriodDays;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    public static SettingsJpaEntity fromEntity(SettingsEntity entity) {
        SettingsJpaEntity jpaEntity = new SettingsJpaEntity();
        jpaEntity.setId(entity.getId());
        jpaEntity.setRetentionPeriodDays(entity.getRetentionPeriodDays());
        jpaEntity.setUpdatedAt(entity.getUpdatedAt());
        jpaEntity.setUpdatedBy(entity.getUpdatedBy());
        return jpaEntity;
    }

    public SettingsEntity toEntity() {
        SettingsEntity entity = new SettingsEntity();
        entity.setId(id);
        entity.setRetentionPeriodDays(retentionPeriodDays);
        entity.setUpdatedAt(updatedAt);
        entity.setUpdatedBy(updatedBy);
        return entity;
    }
}
