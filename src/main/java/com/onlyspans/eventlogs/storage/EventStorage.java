package com.onlyspans.eventlogs.storage;

import com.onlyspans.eventlogs.dto.PagedResult;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.entity.EventEntity;
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

public class EventStorage implements IEventStorage {

    private static final Logger logger = LoggerFactory.getLogger(EventStorage.class);

    private final EventRepository eventRepository;

    @Autowired
    public EventStorage(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public void add(List<EventEntity> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        try {
            eventRepository.saveAll(events);
            logger.info("Successfully saved {} events to storage", events.size());
        } catch (Exception e) {
            logger.error("Error saving events to storage", e);
            throw new EventStorageException("Failed to save events to storage", e);
        }
    }

    @Override
    public PagedResult<EventEntity> search(QueryDto query) {
        try {
            Specification<EventEntity> spec = EventSpecification.buildSpecification(query);

            int page = query.getPage() != null ? query.getPage() : 0;
            int pageSize = query.getSize() != null ? query.getSize() : 20;

            String sortField = query.getSortBy() != null ? query.getSortBy() : "timestamp";
            Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortOrder())
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(page, pageSize, Sort.by(direction, sortField));
            Page<EventEntity> resultPage = eventRepository.findAll(spec, pageable);

            return new PagedResult<>(resultPage.getContent(), resultPage.getTotalElements(), page, pageSize);
        } catch (Exception e) {
            logger.error("Error searching events in storage", e);
            throw new EventSearchException("Failed to search events in storage", e);
        }
    }

    @Override
    public long count(QueryDto query) {
        try {
            Specification<EventEntity> spec = EventSpecification.buildSpecification(query);
            return eventRepository.count(spec);
        } catch (Exception e) {
            logger.error("Error counting events in storage", e);
            throw new EventSearchException("Failed to count events in storage", e);
        }
    }
}
