package com.onlyspans.eventlogs.repository;

import com.onlyspans.eventlogs.entity.jpa.EventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<EventJpaEntity, UUID>, JpaSpecificationExecutor<EventJpaEntity> {

    @Modifying
    @Query("DELETE FROM EventJpaEntity e WHERE e.timestamp < :cutoffDate")
    int deleteEventsOlderThan(@Param("cutoffDate") Instant cutoffDate);
}
