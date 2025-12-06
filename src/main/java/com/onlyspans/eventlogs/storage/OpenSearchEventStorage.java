package com.onlyspans.eventlogs.storage;

import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.entity.EventEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class OpenSearchEventStorage implements IEventStorage {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchEventStorage.class);
    private static final String INDEX_NAME = "event-logs";

    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public OpenSearchEventStorage(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Override
    public void add(List<EventEntity> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        try {
            List<EventEntity> savedEvents = new ArrayList<>();
            for (EventEntity event : events) {
                EventEntity saved = elasticsearchOperations.save(event, IndexCoordinates.of(INDEX_NAME));
                savedEvents.add(saved);
            }
            elasticsearchOperations.indexOps(IndexCoordinates.of(INDEX_NAME)).refresh();
            logger.info("Successfully saved {} events to OpenSearch", savedEvents.size());
        } catch (Exception e) {
            logger.error("Error saving events to OpenSearch", e);
            throw new RuntimeException("Failed to save events to OpenSearch", e);
        }
    }

    @Override
    public List<EventEntity> search(QueryDto query) {
        try {
            Criteria criteria = new Criteria();

            // Add filters
            if (query.getUser() != null && !query.getUser().isEmpty()) {
                criteria = criteria.and(new Criteria("user").is(query.getUser()));
            }
            if (query.getCategory() != null && !query.getCategory().isEmpty()) {
                criteria = criteria.and(new Criteria("category").is(query.getCategory()));
            }
            if (query.getAction() != null && !query.getAction().isEmpty()) {
                criteria = criteria.and(new Criteria("action").is(query.getAction()));
            }
            if (query.getDocument() != null && !query.getDocument().isEmpty()) {
                criteria = criteria.and(new Criteria("document").is(query.getDocument()));
            }
            if (query.getProject() != null && !query.getProject().isEmpty()) {
                criteria = criteria.and(new Criteria("project").is(query.getProject()));
            }
            if (query.getEnvironment() != null && !query.getEnvironment().isEmpty()) {
                criteria = criteria.and(new Criteria("environment").is(query.getEnvironment()));
            }
            if (query.getTenant() != null && !query.getTenant().isEmpty()) {
                criteria = criteria.and(new Criteria("tenant").is(query.getTenant()));
            }
            if (query.getCorrelationId() != null && !query.getCorrelationId().isEmpty()) {
                criteria = criteria.and(new Criteria("correlationId").is(query.getCorrelationId()));
            }
            if (query.getTraceId() != null && !query.getTraceId().isEmpty()) {
                criteria = criteria.and(new Criteria("traceId").is(query.getTraceId()));
            }

            // Date range filter
            if (query.getStartDate() != null || query.getEndDate() != null) {
                Criteria dateCriteria = new Criteria("timestamp");
                if (query.getStartDate() != null) {
                    dateCriteria = dateCriteria.greaterThanEqual(query.getStartDate());
                }
                if (query.getEndDate() != null) {
                    dateCriteria = dateCriteria.lessThanEqual(query.getEndDate());
                }
                criteria = criteria.and(dateCriteria);
            }

            CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);

            // Pagination
            Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
            criteriaQuery.setPageable(pageable);

            // Sorting
            Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortOrder()) 
                ? Sort.Direction.ASC : Sort.Direction.DESC;
            String sortField = query.getSortBy() != null ? query.getSortBy() : "timestamp";
            criteriaQuery.addSort(Sort.by(direction, sortField));

            SearchHits<EventEntity> searchHits = elasticsearchOperations.search(
                criteriaQuery, 
                EventEntity.class, 
                IndexCoordinates.of(INDEX_NAME)
            );

            return searchHits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error searching events in OpenSearch", e);
            throw new RuntimeException("Failed to search events in OpenSearch", e);
        }
    }

    @Override
    public long count(QueryDto query) {
        try {
            Criteria criteria = new Criteria();

            // Add the same filters as in search method
            if (query.getUser() != null && !query.getUser().isEmpty()) {
                criteria = criteria.and(new Criteria("user").is(query.getUser()));
            }
            if (query.getCategory() != null && !query.getCategory().isEmpty()) {
                criteria = criteria.and(new Criteria("category").is(query.getCategory()));
            }
            if (query.getAction() != null && !query.getAction().isEmpty()) {
                criteria = criteria.and(new Criteria("action").is(query.getAction()));
            }
            if (query.getDocument() != null && !query.getDocument().isEmpty()) {
                criteria = criteria.and(new Criteria("document").is(query.getDocument()));
            }
            if (query.getProject() != null && !query.getProject().isEmpty()) {
                criteria = criteria.and(new Criteria("project").is(query.getProject()));
            }
            if (query.getEnvironment() != null && !query.getEnvironment().isEmpty()) {
                criteria = criteria.and(new Criteria("environment").is(query.getEnvironment()));
            }
            if (query.getTenant() != null && !query.getTenant().isEmpty()) {
                criteria = criteria.and(new Criteria("tenant").is(query.getTenant()));
            }
            if (query.getCorrelationId() != null && !query.getCorrelationId().isEmpty()) {
                criteria = criteria.and(new Criteria("correlationId").is(query.getCorrelationId()));
            }
            if (query.getTraceId() != null && !query.getTraceId().isEmpty()) {
                criteria = criteria.and(new Criteria("traceId").is(query.getTraceId()));
            }

            if (query.getStartDate() != null || query.getEndDate() != null) {
                Criteria dateCriteria = new Criteria("timestamp");
                if (query.getStartDate() != null) {
                    dateCriteria = dateCriteria.greaterThanEqual(query.getStartDate());
                }
                if (query.getEndDate() != null) {
                    dateCriteria = dateCriteria.lessThanEqual(query.getEndDate());
                }
                criteria = criteria.and(dateCriteria);
            }

            CriteriaQuery criteriaQuery = new CriteriaQuery(criteria);
            criteriaQuery.setPageable(PageRequest.of(0, 1)); // Minimal pageable for count

            SearchHits<EventEntity> searchHits = elasticsearchOperations.search(
                criteriaQuery, 
                EventEntity.class, 
                IndexCoordinates.of(INDEX_NAME)
            );

            return searchHits.getTotalHits();
        } catch (Exception e) {
            logger.error("Error counting events in OpenSearch", e);
            throw new RuntimeException("Failed to count events in OpenSearch", e);
        }
    }
}

