package com.onlyspans.eventlogs.service;

import com.onlyspans.eventlogs.dto.EventDto;
import com.onlyspans.eventlogs.dto.PagedResult;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.dto.QueryResult;
import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.storage.IEventStorage;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private IEventStorage eventStorage;

    private SimpleMeterRegistry meterRegistry;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        eventService = new EventService(eventStorage, meterRegistry);
        ReflectionTestUtils.setField(eventService, "maxExportSize", 10000);
    }

    @Test
    void ingestEvents_shouldConvertDtosToEntitiesAndStore() {
        // Given
        EventDto dto = createEventDto();
        List<EventDto> dtos = List.of(dto);

        // When
        eventService.ingestEvents(dtos);

        // Then
        ArgumentCaptor<List<EventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventStorage).add(captor.capture());

        List<EventEntity> capturedEntities = captor.getValue();
        assertEquals(1, capturedEntities.size());

        EventEntity entity = capturedEntities.get(0);
        assertEquals(dto.getUser(), entity.getUser());
        assertEquals(dto.getCategory(), entity.getCategory());
        assertEquals(dto.getAction(), entity.getAction());
        assertEquals(dto.getDocument(), entity.getDocumentName());
        assertEquals(dto.getProject(), entity.getProject());
        assertEquals(dto.getEnvironment(), entity.getEnvironment());
        assertEquals(dto.getTenant(), entity.getTenant());
        assertEquals(dto.getCorrelationId(), entity.getCorrelationId());
        assertEquals(dto.getTraceId(), entity.getTraceId());
    }

    @Test
    void ingestEvents_shouldHandleUuidIdConversion() {
        // Given
        EventDto dto = createEventDto();
        String validUuid = UUID.randomUUID().toString();
        dto.setId(validUuid);
        List<EventDto> dtos = List.of(dto);

        // When
        eventService.ingestEvents(dtos);

        // Then
        ArgumentCaptor<List<EventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventStorage).add(captor.capture());

        EventEntity entity = captor.getValue().get(0);
        assertEquals(UUID.fromString(validUuid), entity.getId());
    }

    @Test
    void ingestEvents_shouldHandleInvalidUuidGracefully() {
        // Given
        EventDto dto = createEventDto();
        dto.setId("invalid-uuid");
        List<EventDto> dtos = List.of(dto);

        // When
        eventService.ingestEvents(dtos);

        // Then
        ArgumentCaptor<List<EventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventStorage).add(captor.capture());

        EventEntity entity = captor.getValue().get(0);
        assertNull(entity.getId()); // Should be null since JPA will generate one
    }

    @Test
    void ingestEvents_shouldSetCurrentTimestampIfNotProvided() {
        // Given
        EventDto dto = createEventDto();
        dto.setTimestamp(null);
        List<EventDto> dtos = List.of(dto);
        Instant before = Instant.now();

        // When
        eventService.ingestEvents(dtos);

        // Then
        Instant after = Instant.now();
        ArgumentCaptor<List<EventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventStorage).add(captor.capture());

        EventEntity entity = captor.getValue().get(0);
        assertNotNull(entity.getTimestamp());
        assertTrue(entity.getTimestamp().isAfter(before.minusSeconds(1)));
        assertTrue(entity.getTimestamp().isBefore(after.plusSeconds(1)));
    }

    @Test
    void searchEvents_shouldReturnMappedResults() {
        // Given
        QueryDto query = new QueryDto();
        query.setUser("test-user");

        EventEntity entity = createEventEntity();
        PagedResult<EventEntity> pagedResult = new PagedResult<>(
            List.of(entity),
            1L,
            0,
            20
        );

        when(eventStorage.search(any(QueryDto.class))).thenReturn(pagedResult);

        // When
        QueryResult result = eventService.searchEvents(query);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getEvents().size());
        assertEquals(1L, result.getTotal());
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());

        EventDto resultDto = result.getEvents().get(0);
        assertEquals(entity.getId().toString(), resultDto.getId());
        assertEquals(entity.getUser(), resultDto.getUser());
        assertEquals(entity.getCategory(), resultDto.getCategory());
        assertEquals(entity.getAction(), resultDto.getAction());
    }

    @Test
    void exportCsv_shouldWriteHeadersAndData() throws IOException {
        // Given
        QueryDto query = new QueryDto();
        EventEntity entity = createEventEntity();
        PagedResult<EventEntity> pagedResult = new PagedResult<>(
            List.of(entity),
            1L,
            0,
            10000
        );

        when(eventStorage.search(any(QueryDto.class))).thenReturn(pagedResult);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // When
        eventService.exportCsv(query, outputStream);

        // Then
        String csvContent = outputStream.toString();
        assertNotNull(csvContent);
        assertTrue(csvContent.contains("ID"));
        assertTrue(csvContent.contains("Timestamp"));
        assertTrue(csvContent.contains("User"));
        assertTrue(csvContent.contains(entity.getId().toString()));
        assertTrue(csvContent.contains(entity.getUser()));
    }

    @Test
    void exportCsv_shouldSetMaxExportSizeLimit() {
        // Given
        QueryDto query = new QueryDto();

        PagedResult<EventEntity> pagedResult = new PagedResult<>(
            List.of(),
            0L,
            0,
            10000
        );
        when(eventStorage.search(any(QueryDto.class))).thenReturn(pagedResult);

        // When
        eventService.exportCsv(query, new ByteArrayOutputStream());

        // Then
        ArgumentCaptor<QueryDto> captor = ArgumentCaptor.forClass(QueryDto.class);
        verify(eventStorage).search(captor.capture());

        QueryDto capturedQuery = captor.getValue();
        assertEquals(10000, capturedQuery.getSize()); // Should be set to maxExportSize
        assertEquals(0, capturedQuery.getPage()); // Should reset page to 0
    }

    private EventDto createEventDto() {
        EventDto dto = new EventDto();
        dto.setTimestamp(Instant.now());
        dto.setUser("test-user");
        dto.setCategory("test-category");
        dto.setAction("test-action");
        dto.setDocument("test-document");
        dto.setProject("test-project");
        dto.setEnvironment("test-env");
        dto.setTenant("test-tenant");
        dto.setCorrelationId("test-correlation-id");
        dto.setTraceId("test-trace-id");
        return dto;
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
        entity.setCorrelationId("test-correlation-id");
        entity.setTraceId("test-trace-id");
        return entity;
    }
}
