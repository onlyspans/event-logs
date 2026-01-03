package com.onlyspans.eventlogs.storage;

import com.onlyspans.eventlogs.dto.PagedResult;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.exception.EventSearchException;
import com.onlyspans.eventlogs.exception.EventStorageException;
import com.onlyspans.eventlogs.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventStorageTest {

    @Mock
    private EventRepository eventRepository;

    private EventStorage eventStorage;

    @BeforeEach
    void setUp() {
        eventStorage = new EventStorage(eventRepository);
    }

    @Test
    void add_shouldSaveAllEvents() {
        // Given
        EventEntity entity1 = createEventEntity();
        EventEntity entity2 = createEventEntity();
        List<EventEntity> events = List.of(entity1, entity2);

        when(eventRepository.saveAll(anyList())).thenReturn(events);

        // When
        eventStorage.add(events);

        // Then
        verify(eventRepository).saveAll(events);
    }

    @Test
    void add_shouldNotSaveWhenEventsNull() {
        // When
        eventStorage.add(null);

        // Then
        verify(eventRepository, never()).saveAll(any());
    }

    @Test
    void add_shouldNotSaveWhenEventsEmpty() {
        // When
        eventStorage.add(List.of());

        // Then
        verify(eventRepository, never()).saveAll(any());
    }

    @Test
    void add_shouldThrowEventStorageExceptionOnError() {
        // Given
        List<EventEntity> events = List.of(createEventEntity());
        when(eventRepository.saveAll(anyList())).thenThrow(new RuntimeException("Database error"));

        // When/Then
        assertThrows(EventStorageException.class, () -> eventStorage.add(events));
    }

    @Test
    void search_shouldReturnPagedResults() {
        // Given
        QueryDto query = new QueryDto();
        query.setPage(0);
        query.setSize(10);

        EventEntity entity = createEventEntity();
        Page<EventEntity> page = new PageImpl<>(List.of(entity), Pageable.ofSize(10), 1);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // When
        PagedResult<EventEntity> result = eventStorage.search(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getTotal());
        assertEquals(0, result.getPage());
        assertEquals(10, result.getPageSize());
    }

    @Test
    void search_shouldUseDefaultPageAndSize() {
        // Given
        QueryDto query = new QueryDto();
        Page<EventEntity> page = new PageImpl<>(List.of(), Pageable.ofSize(20), 0);

        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // When
        PagedResult<EventEntity> result = eventStorage.search(query);

        // Then
        assertEquals(0, result.getPage());
        assertEquals(20, result.getPageSize());
    }

    @Test
    void search_shouldApplySortingAscending() {
        // Given
        QueryDto query = new QueryDto();
        query.setSortBy("timestamp");
        query.setSortOrder("asc");

        Page<EventEntity> page = new PageImpl<>(List.of());
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // When
        eventStorage.search(query);

        // Then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(any(Specification.class), captor.capture());

        Pageable pageable = captor.getValue();
        assertTrue(pageable.getSort().getOrderFor("timestamp").isAscending());
    }

    @Test
    void search_shouldApplySortingDescending() {
        // Given
        QueryDto query = new QueryDto();
        query.setSortBy("timestamp");
        query.setSortOrder("desc");

        Page<EventEntity> page = new PageImpl<>(List.of());
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        // When
        eventStorage.search(query);

        // Then
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(any(Specification.class), captor.capture());

        Pageable pageable = captor.getValue();
        assertTrue(pageable.getSort().getOrderFor("timestamp").isDescending());
    }

    @Test
    void search_shouldThrowEventSearchExceptionOnError() {
        // Given
        QueryDto query = new QueryDto();
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When/Then
        assertThrows(EventSearchException.class, () -> eventStorage.search(query));
    }

    @Test
    void count_shouldReturnTotalCount() {
        // Given
        QueryDto query = new QueryDto();
        query.setUser("test-user");

        when(eventRepository.count(any(Specification.class))).thenReturn(42L);

        // When
        long count = eventStorage.count(query);

        // Then
        assertEquals(42L, count);
        verify(eventRepository).count(any(Specification.class));
    }

    @Test
    void count_shouldThrowEventSearchExceptionOnError() {
        // Given
        QueryDto query = new QueryDto();
        when(eventRepository.count(any(Specification.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When/Then
        assertThrows(EventSearchException.class, () -> eventStorage.count(query));
    }

    private EventEntity createEventEntity() {
        EventEntity entity = new EventEntity();
        entity.setId(UUID.randomUUID());
        entity.setTimestamp(Instant.now());
        entity.setUser("test-user");
        entity.setCategory("test-category");
        entity.setAction("test-action");
        entity.setDocumentName("test-document");
        entity.setProject("test-project");
        entity.setEnvironment("test-env");
        entity.setTenant("test-tenant");
        return entity;
    }
}
