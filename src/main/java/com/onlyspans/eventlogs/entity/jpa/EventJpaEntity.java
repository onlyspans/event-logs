package com.onlyspans.eventlogs.entity.jpa;

import com.onlyspans.eventlogs.entity.EventEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "user_name")
    private String user;

    private String category;
    private String action;

    @Column(name = "document_name")
    private String documentName;

    private String project;
    private String environment;
    private String tenant;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "trace_id")
    private String traceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private EventEntity.EventDetails details;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public static EventJpaEntity fromEntity(EventEntity entity) {
        EventJpaEntity jpaEntity = new EventJpaEntity();
        if (entity.getId() != null && !entity.getId().isEmpty()) {
            try {
                jpaEntity.setId(UUID.fromString(entity.getId()));
            } catch (IllegalArgumentException e) {
                // If ID is not a valid UUID, let it be generated
            }
        }
        jpaEntity.setTimestamp(entity.getTimestamp());
        jpaEntity.setUser(entity.getUser());
        jpaEntity.setCategory(entity.getCategory());
        jpaEntity.setAction(entity.getAction());
        jpaEntity.setDocumentName(entity.getDocumentName());
        jpaEntity.setProject(entity.getProject());
        jpaEntity.setEnvironment(entity.getEnvironment());
        jpaEntity.setTenant(entity.getTenant());
        jpaEntity.setCorrelationId(entity.getCorrelationId());
        jpaEntity.setTraceId(entity.getTraceId());
        jpaEntity.setDetails(entity.getDetails());
        return jpaEntity;
    }

    public EventEntity toEntity() {
        EventEntity entity = new EventEntity();
        entity.setId(id != null ? id.toString() : null);
        entity.setTimestamp(timestamp);
        entity.setUser(user);
        entity.setCategory(category);
        entity.setAction(action);
        entity.setDocumentName(documentName);
        entity.setProject(project);
        entity.setEnvironment(environment);
        entity.setTenant(tenant);
        entity.setCorrelationId(correlationId);
        entity.setTraceId(traceId);
        entity.setDetails(details);
        return entity;
    }
}
