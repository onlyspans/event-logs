package com.onlyspans.eventlogs.controller;

import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.dto.QueryResult;
import com.onlyspans.eventlogs.dto.SearchEventsRequest;
import com.onlyspans.eventlogs.mapper.EventMapper;
import com.onlyspans.eventlogs.service.IEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private IEventService eventService;

    @Mock
    private EventMapper eventMapper;

    private EventController eventController;

    @BeforeEach
    void setUp() {
        eventController = new EventController(eventService, eventMapper);
    }

    @Test
    void searchEvents_shouldReturnResults() {
        // Given
        SearchEventsRequest request = new SearchEventsRequest();
        request.setUser("test-user");
        request.setCategory("test-category");

        QueryDto queryDto = new QueryDto();
        queryDto.setUser("test-user");
        queryDto.setCategory("test-category");

        EventDto dto = new EventDto();
        dto.setUser("test-user");
        dto.setCategory("test-category");

        QueryResult result = new QueryResult(List.of(dto), 1L, 0, 20);
        when(eventMapper.toQueryDto(request)).thenReturn(queryDto);
        when(eventService.searchEvents(any(QueryDto.class))).thenReturn(result);

        // When
        QueryResult actualResult = eventController.searchEvents(request);

        // Then
        assertNotNull(actualResult);
        assertEquals(1, actualResult.getEvents().size());
        assertEquals(1L, actualResult.getTotal());
        assertEquals("test-user", actualResult.getEvents().get(0).getUser());
        verify(eventMapper).toQueryDto(request);
        verify(eventService).searchEvents(queryDto);
    }

    @Test
    void searchEvents_shouldPassAllParametersToService() {
        // Given
        SearchEventsRequest request = new SearchEventsRequest();
        request.setUser("user1");
        request.setCategory("cat1");
        request.setAction("action1");
        request.setDocument("doc1");
        request.setProject("proj1");
        request.setEnvironment("env1");
        request.setTenant("tenant1");
        request.setCorrelationId("corr-123");
        request.setTraceId("trace-456");
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        request.setStartDate(start);
        request.setEndDate(end);
        request.setSortBy("user");
        request.setSortOrder("asc");
        request.setPage(1);
        request.setSize(50);

        QueryDto queryDto = new QueryDto();
        queryDto.setUser("user1");
        queryDto.setCategory("cat1");
        queryDto.setAction("action1");
        queryDto.setDocument("doc1");
        queryDto.setProject("proj1");
        queryDto.setEnvironment("env1");
        queryDto.setTenant("tenant1");
        queryDto.setCorrelationId("corr-123");
        queryDto.setTraceId("trace-456");
        queryDto.setStartDate(start);
        queryDto.setEndDate(end);
        queryDto.setSortBy("user");
        queryDto.setSortOrder("asc");
        queryDto.setPage(1);
        queryDto.setSize(50);

        QueryResult result = new QueryResult(List.of(), 0L, 0, 20);
        when(eventMapper.toQueryDto(request)).thenReturn(queryDto);
        when(eventService.searchEvents(any(QueryDto.class))).thenReturn(result);

        // When
        eventController.searchEvents(request);

        // Then
        verify(eventMapper).toQueryDto(request);
        ArgumentCaptor<QueryDto> captor = ArgumentCaptor.forClass(QueryDto.class);
        verify(eventService).searchEvents(captor.capture());

        QueryDto capturedQuery = captor.getValue();
        assertEquals("user1", capturedQuery.getUser());
        assertEquals("cat1", capturedQuery.getCategory());
        assertEquals("action1", capturedQuery.getAction());
        assertEquals("doc1", capturedQuery.getDocument());
        assertEquals("proj1", capturedQuery.getProject());
        assertEquals("env1", capturedQuery.getEnvironment());
        assertEquals("tenant1", capturedQuery.getTenant());
        assertEquals("corr-123", capturedQuery.getCorrelationId());
        assertEquals("trace-456", capturedQuery.getTraceId());
        assertEquals(start, capturedQuery.getStartDate());
        assertEquals(end, capturedQuery.getEndDate());
        assertEquals("user", capturedQuery.getSortBy());
        assertEquals("asc", capturedQuery.getSortOrder());
        assertEquals(1, capturedQuery.getPage());
        assertEquals(50, capturedQuery.getSize());
    }

    @Test
    void searchEvents_shouldUseDefaultParametersWhenNullProvided() {
        // Given
        SearchEventsRequest request = new SearchEventsRequest();
        request.setSortBy("timestamp");
        request.setSortOrder("desc");
        request.setPage(0);
        request.setSize(20);

        QueryDto queryDto = new QueryDto();
        queryDto.setSortBy("timestamp");
        queryDto.setSortOrder("desc");
        queryDto.setPage(0);
        queryDto.setSize(20);

        QueryResult result = new QueryResult(List.of(), 0L, 0, 20);
        when(eventMapper.toQueryDto(request)).thenReturn(queryDto);
        when(eventService.searchEvents(any(QueryDto.class))).thenReturn(result);

        // When
        eventController.searchEvents(request);

        // Then
        verify(eventMapper).toQueryDto(request);
        ArgumentCaptor<QueryDto> captor = ArgumentCaptor.forClass(QueryDto.class);
        verify(eventService).searchEvents(captor.capture());

        QueryDto capturedQuery = captor.getValue();
        assertNull(capturedQuery.getUser());
        assertNull(capturedQuery.getCategory());
        assertEquals("timestamp", capturedQuery.getSortBy());
        assertEquals("desc", capturedQuery.getSortOrder());
        assertEquals(0, capturedQuery.getPage());
        assertEquals(20, capturedQuery.getSize());
    }
}
