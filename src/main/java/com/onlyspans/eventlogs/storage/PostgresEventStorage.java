package com.onlyspans.eventlogs.storage;

import com.onlyspans.eventlogs.dto.PagedResult;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.entity.jpa.EventJpaEntity;
import com.onlyspans.eventlogs.exception.EventSearchException;
import com.onlyspans.eventlogs.exception.EventStorageException;
import com.onlyspans.eventlogs.repository.EventRepository;
import com.onlyspans.eventlogs.repository.EventSpecification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Collectors;

public class PostgresEventStorage implements IEventStorage {

    private static final Logger logger = LoggerFactory.getLogger(PostgresEventStorage.class);

    private final EventRepository eventRepository;

    @Autowired
    public PostgresEventStorage(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void add(List<EventEntity> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        try {
            List<EventJpaEntity> jpaEntities = events.stream()
                    .map(EventJpaEntity::fromEntity)
                    .collect(Collectors.toList());

            eventRepository.saveAll(jpaEntities);
            logger.info("Successfully saved {} events to PostgreSQL", events.size());
        } catch (Exception e) {
            logger.error("Error saving events to PostgreSQL", e);
            throw new EventStorageException("Failed to save events to PostgreSQL", e);
        }
    }

    @Override
    public PagedResult<EventEntity> search(QueryDto query) {
        try {
            Specification<EventJpaEntity> spec = EventSpecification.buildSpecification(query);

            int page = query.getPage() != null ? query.getPage() : 0;
            int pageSize = query.getSize() != null ? query.getSize() : 20;

            String sortField = query.getSortBy() != null ? query.getSortBy() : "timestamp";
            Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortOrder())
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortField));
            Page<EventJpaEntity> resultPage = eventRepository.findAll(spec, pageable);

            List<EventEntity> results = resultPage.getContent().stream()
                    .map(EventJpaEntity::toEntity)
                    .collect(Collectors.toList());

            return new PagedResult<>(results, resultPage.getTotalElements(), page, pageSize);
        } catch (Exception e) {
            logger.error("Error searching events in PostgreSQL", e);
            throw new EventSearchException("Failed to search events in PostgreSQL", e);
        }
    }

    @Override
    public long count(QueryDto query) {
        try {
            Specification<EventJpaEntity> spec = EventSpecification.buildSpecification(query);
            return eventRepository.count(spec);
        } catch (Exception e) {
            logger.error("Error counting events in PostgreSQL", e);
            throw new EventSearchException("Failed to count events in PostgreSQL", e);
        }
    }
}
