package com.onlyspans.eventlogs.repository;

import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.entity.jpa.EventJpaEntity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<EventJpaEntity> buildSpecification(QueryDto query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.getUser() != null && !query.getUser().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("user"), query.getUser()));
            }

            if (query.getCategory() != null && !query.getCategory().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("category"), query.getCategory()));
            }

            if (query.getAction() != null && !query.getAction().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("action"), query.getAction()));
            }

            if (query.getDocument() != null && !query.getDocument().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("documentName"), query.getDocument()));
            }

            if (query.getProject() != null && !query.getProject().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("project"), query.getProject()));
            }

            if (query.getEnvironment() != null && !query.getEnvironment().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("environment"), query.getEnvironment()));
            }

            if (query.getTenant() != null && !query.getTenant().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("tenant"), query.getTenant()));
            }

            if (query.getCorrelationId() != null && !query.getCorrelationId().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("correlationId"), query.getCorrelationId()));
            }

            if (query.getTraceId() != null && !query.getTraceId().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("traceId"), query.getTraceId()));
            }

            if (query.getStartDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), query.getStartDate()));
            }

            if (query.getEndDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), query.getEndDate()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
