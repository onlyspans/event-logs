package com.onlyspans.eventlogs.integration;

import com.onlyspans.eventlogs.dto.ExportEventsRequest;
import com.onlyspans.eventlogs.dto.QueryResult;
import com.onlyspans.eventlogs.dto.SearchEventsRequest;
import com.onlyspans.eventlogs.entity.EventEntity;
import com.onlyspans.eventlogs.repository.EventRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class EventControllerIntegrationTest extends BaseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private EventRepository eventRepository;

    private RestTemplate restTemplate = new RestTemplate();

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        eventRepository.deleteAll();
    }

    @Test
    void searchEvents_shouldReturnAllEvents() {
        // Given
        createTestEvent("user1", "category1", "action1", "doc1", "proj1", "env1", "tenant1");
        createTestEvent("user2", "category2", "action2", "doc2", "proj2", "env2", "tenant2");

        SearchEventsRequest request = new SearchEventsRequest();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEvents().size());
        assertEquals(2L, response.getBody().getTotal());
    }

    @Test
    void searchEvents_shouldFilterByUser() {
        // Given
        createTestEvent("alice", "category1", "action1", "doc1", "proj1", "env1", "tenant1");
        createTestEvent("bob", "category2", "action2", "doc2", "proj2", "env2", "tenant2");
        createTestEvent("alice", "category3", "action3", "doc3", "proj3", "env3", "tenant3");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setUser("alice");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEvents().size());
        assertTrue(response.getBody().getEvents().stream()
                .allMatch(e -> "alice".equals(e.getUser())));
    }

    @Test
    void searchEvents_shouldFilterByCategory() {
        // Given
        createTestEvent("user1", "authentication", "login", "doc1", "proj1", "env1", "tenant1");
        createTestEvent("user2", "data-access", "read", "doc2", "proj2", "env2", "tenant2");
        createTestEvent("user3", "authentication", "logout", "doc3", "proj3", "env3", "tenant3");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setCategory("authentication");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEvents().size());
        assertTrue(response.getBody().getEvents().stream()
                .allMatch(e -> "authentication".equals(e.getCategory())));
    }

    @Test
    void searchEvents_shouldFilterByMultipleCriteria() {
        // Given
        createTestEvent("alice", "auth", "login", "doc1", "project-a", "prod", "tenant1");
        createTestEvent("alice", "auth", "logout", "doc2", "project-a", "prod", "tenant1");
        createTestEvent("bob", "auth", "login", "doc3", "project-a", "prod", "tenant1");
        createTestEvent("alice", "data", "read", "doc4", "project-b", "dev", "tenant2");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setUser("alice");
        request.setCategory("auth");
        request.setProject("project-a");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When - filter by user, category, and project
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEvents().size());
        assertTrue(response.getBody().getEvents().stream()
                .allMatch(e -> "alice".equals(e.getUser())
                        && "auth".equals(e.getCategory())
                        && "project-a".equals(e.getProject())));
    }

    @Test
    void searchEvents_shouldFilterByDateRange() {
        // Given
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);

        createTestEventWithTimestamp("user1", "cat1", "act1", twoDaysAgo);
        createTestEventWithTimestamp("user2", "cat2", "act2", yesterday);
        createTestEventWithTimestamp("user3", "cat3", "act3", now);

        // When - filter last 36 hours
        Instant startDate = now.minus(36, ChronoUnit.HOURS);
        SearchEventsRequest request = new SearchEventsRequest();
        request.setStartDate(startDate);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEvents().size());
    }

    @Test
    void searchEvents_shouldFilterByAction() {
        // Given
        createTestEvent("user1", "cat1", "create", "doc1", "proj1", "env1", "tenant1");
        createTestEvent("user2", "cat2", "update", "doc2", "proj2", "env2", "tenant2");
        createTestEvent("user3", "cat3", "delete", "doc3", "proj3", "env3", "tenant3");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setAction("update");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getEvents().size());
        assertEquals("update", response.getBody().getEvents().get(0).getAction());
    }

    @Test
    void searchEvents_shouldFilterByDocument() {
        // Given
        createTestEvent("user1", "cat1", "act1", "invoice-123", "proj1", "env1", "tenant1");
        createTestEvent("user2", "cat2", "act2", "contract-456", "proj2", "env2", "tenant2");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setDocument("invoice-123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getEvents().size());
        assertEquals("invoice-123", response.getBody().getEvents().get(0).getDocument());
    }

    @Test
    void searchEvents_shouldFilterByProject() {
        // Given
        createTestEvent("user1", "cat1", "act1", "doc1", "frontend", "env1", "tenant1");
        createTestEvent("user2", "cat2", "act2", "doc2", "backend", "env2", "tenant2");
        createTestEvent("user3", "cat3", "act3", "doc3", "frontend", "env3", "tenant3");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setProject("frontend");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEvents().size());
        assertTrue(response.getBody().getEvents().stream()
                .allMatch(e -> "frontend".equals(e.getProject())));
    }

    @Test
    void searchEvents_shouldFilterByEnvironment() {
        // Given
        createTestEvent("user1", "cat1", "act1", "doc1", "proj1", "production", "tenant1");
        createTestEvent("user2", "cat2", "act2", "doc2", "proj2", "staging", "tenant2");
        createTestEvent("user3", "cat3", "act3", "doc3", "proj3", "production", "tenant3");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setEnvironment("production");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEvents().size());
        assertTrue(response.getBody().getEvents().stream()
                .allMatch(e -> "production".equals(e.getEnvironment())));
    }

    @Test
    void searchEvents_shouldFilterByTenant() {
        // Given
        createTestEvent("user1", "cat1", "act1", "doc1", "proj1", "env1", "acme-corp");
        createTestEvent("user2", "cat2", "act2", "doc2", "proj2", "env2", "widgets-inc");
        createTestEvent("user3", "cat3", "act3", "doc3", "proj3", "env3", "acme-corp");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setTenant("acme-corp");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getEvents().size());
        assertTrue(response.getBody().getEvents().stream()
                .allMatch(e -> "acme-corp".equals(e.getTenant())));
    }

    @Test
    void searchEvents_shouldFilterByCorrelationId() {
        // Given
        EventEntity event1 = createTestEvent("user1", "cat1", "act1", "doc1", "proj1", "env1", "tenant1");
        event1.setCorrelationId("corr-123");
        eventRepository.save(event1);

        EventEntity event2 = createTestEvent("user2", "cat2", "act2", "doc2", "proj2", "env2", "tenant2");
        event2.setCorrelationId("corr-456");
        eventRepository.save(event2);

        SearchEventsRequest request = new SearchEventsRequest();
        request.setCorrelationId("corr-123");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getEvents().size());
        assertEquals("corr-123", response.getBody().getEvents().get(0).getCorrelationId());
    }

    @Test
    void searchEvents_shouldFilterByTraceId() {
        // Given
        EventEntity event1 = createTestEvent("user1", "cat1", "act1", "doc1", "proj1", "env1", "tenant1");
        event1.setTraceId("trace-abc");
        eventRepository.save(event1);

        EventEntity event2 = createTestEvent("user2", "cat2", "act2", "doc2", "proj2", "env2", "tenant2");
        event2.setTraceId("trace-xyz");
        eventRepository.save(event2);

        SearchEventsRequest request = new SearchEventsRequest();
        request.setTraceId("trace-abc");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getEvents().size());
        assertEquals("trace-abc", response.getBody().getEvents().get(0).getTraceId());
    }

    @Test
    void searchEvents_shouldSupportPagination() {
        // Given - create 25 events
        for (int i = 0; i < 25; i++) {
            createTestEvent("user" + i, "cat", "act", "doc", "proj", "env", "tenant");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // When - get first page (10 items)
        SearchEventsRequest request1 = new SearchEventsRequest();
        request1.setPage(0);
        request1.setSize(10);
        HttpEntity<SearchEventsRequest> entity1 = new HttpEntity<>(request1, headers);
        ResponseEntity<QueryResult> page1 = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity1,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, page1.getStatusCode());
        assertNotNull(page1.getBody());
        assertEquals(10, page1.getBody().getEvents().size());
        assertEquals(25L, page1.getBody().getTotal());
        assertEquals(0, page1.getBody().getPage());
        assertEquals(10, page1.getBody().getSize());

        // When - get second page
        SearchEventsRequest request2 = new SearchEventsRequest();
        request2.setPage(1);
        request2.setSize(10);
        HttpEntity<SearchEventsRequest> entity2 = new HttpEntity<>(request2, headers);
        ResponseEntity<QueryResult> page2 = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity2,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, page2.getStatusCode());
        assertNotNull(page2.getBody());
        assertEquals(10, page2.getBody().getEvents().size());
        assertEquals(1, page2.getBody().getPage());
    }

    @Test
    void searchEvents_shouldSortAscending() {
        // Given
        createTestEvent("charlie", "cat1", "act1", "doc1", "proj1", "env1", "tenant1");
        createTestEvent("alice", "cat2", "act2", "doc2", "proj2", "env2", "tenant2");
        createTestEvent("bob", "cat3", "act3", "doc3", "proj3", "env3", "tenant3");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setSortBy("user");
        request.setSortOrder("asc");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getEvents().size());
        assertEquals("alice", response.getBody().getEvents().get(0).getUser());
        assertEquals("bob", response.getBody().getEvents().get(1).getUser());
        assertEquals("charlie", response.getBody().getEvents().get(2).getUser());
    }

    @Test
    void searchEvents_shouldSortDescending() {
        // Given
        createTestEvent("alice", "cat1", "act1", "doc1", "proj1", "env1", "tenant1");
        createTestEvent("bob", "cat2", "act2", "doc2", "proj2", "env2", "tenant2");
        createTestEvent("charlie", "cat3", "act3", "doc3", "proj3", "env3", "tenant3");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setSortBy("user");
        request.setSortOrder("desc");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getEvents().size());
        assertEquals("charlie", response.getBody().getEvents().get(0).getUser());
        assertEquals("bob", response.getBody().getEvents().get(1).getUser());
        assertEquals("alice", response.getBody().getEvents().get(2).getUser());
    }

    @Test
    void searchEvents_shouldReturnEmptyWhenNoMatches() {
        // Given
        createTestEvent("user1", "cat1", "act1", "doc1", "proj1", "env1", "tenant1");

        SearchEventsRequest request = new SearchEventsRequest();
        request.setUser("nonexistent");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchEventsRequest> entity = new HttpEntity<>(request, headers);

        // When
        ResponseEntity<QueryResult> response = restTemplate.exchange(
                getBaseUrl() + "/events",
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<QueryResult>() {}
        );

        // Then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getEvents().size());
        assertEquals(0L, response.getBody().getTotal());
    }

    private EventEntity createTestEvent(String user, String category, String action,
                                        String document, String project, String environment, String tenant) {
        EventEntity event = new EventEntity();
        event.setTimestamp(Instant.now());
        event.setUser(user);
        event.setCategory(category);
        event.setAction(action);
        event.setDocumentName(document);
        event.setProject(project);
        event.setEnvironment(environment);
        event.setTenant(tenant);
        return eventRepository.save(event);
    }

    private EventEntity createTestEventWithTimestamp(String user, String category, String action, Instant timestamp) {
        EventEntity event = new EventEntity();
        event.setTimestamp(timestamp);
        event.setUser(user);
        event.setCategory(category);
        event.setAction(action);
        event.setDocumentName("doc");
        event.setProject("proj");
        event.setEnvironment("env");
        event.setTenant("tenant");
        return eventRepository.save(event);
    }
}
