package com.onlyspans.eventlogs.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "events")
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(name = "user_name")
    private String user;

    private String category;
    private String action;

    @JsonProperty("document")
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
    private EventDetails details;

    @Data
    public static class EventDetails {
        private List<Change> changes;
        private String ipAddress;
        private String userAgent;
        private String additionalInfo;
    }

    @Data
    public static class Change {
        private String field;
        private String oldValue;
        private String newValue;
    }
}

