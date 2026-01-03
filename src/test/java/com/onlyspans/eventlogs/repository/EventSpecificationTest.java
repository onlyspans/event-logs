package com.onlyspans.eventlogs.repository;

import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.entity.EventEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class EventSpecificationTest {

    @Test
    void buildSpecification_shouldReturnNonNullSpecification() {
        // Given
        QueryDto query = new QueryDto();

        // When
        Specification<EventEntity> spec = EventSpecification.buildSpecification(query);

        // Then
        assertNotNull(spec);
    }

    @Test
    void buildSpecification_shouldAcceptAllQueryParameters() {
        // Given
        QueryDto query = new QueryDto();
        query.setUser("test-user");
        query.setCategory("test-category");
        query.setAction("test-action");
        query.setDocument("test-document");
        query.setProject("test-project");
        query.setEnvironment("test-env");
        query.setTenant("test-tenant");
        query.setCorrelationId("corr-123");
        query.setTraceId("trace-456");
        query.setStartDate(Instant.now().minusSeconds(3600));
        query.setEndDate(Instant.now());

        // When
        Specification<EventEntity> spec = EventSpecification.buildSpecification(query);

        // Then
        assertNotNull(spec);
    }

    @Test
    void buildSpecification_shouldHandleNullParameters() {
        // Given
        QueryDto query = new QueryDto();

        // When/Then - should not throw exception
        assertDoesNotThrow(() -> EventSpecification.buildSpecification(query));
    }

    @Test
    void buildSpecification_shouldHandleEmptyStrings() {
        // Given
        QueryDto query = new QueryDto();
        query.setUser("");
        query.setCategory("");

        // When
        Specification<EventEntity> spec = EventSpecification.buildSpecification(query);

        // Then
        assertNotNull(spec);
    }
}
