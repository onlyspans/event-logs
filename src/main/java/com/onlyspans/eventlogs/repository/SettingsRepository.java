package com.onlyspans.eventlogs.repository;

import com.onlyspans.eventlogs.entity.jpa.SettingsJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingsRepository extends JpaRepository<SettingsJpaEntity, String> {
}
