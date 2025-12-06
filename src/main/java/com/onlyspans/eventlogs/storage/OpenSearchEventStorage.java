package com.onlyspans.eventlogs.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyspans.eventlogs.dto.QueryDto;
import com.onlyspans.eventlogs.entity.EventEntity;
import org.opensearch.action.bulk.BulkRequest;
import org.opensearch.action.bulk.BulkResponse;
import org.opensearch.action.index.IndexRequest;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.action.support.WriteRequest;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.core.CountRequest;
import org.opensearch.client.core.CountResponse;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.RangeQueryBuilder;
import org.opensearch.search.SearchHit;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
public class OpenSearchEventStorage implements IEventStorage {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchEventStorage.class);
    private static final String INDEX_NAME = "event-logs";

    private final RestHighLevelClient client;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenSearchEventStorage(RestHighLevelClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Override
    public void add(List<EventEntity> events) {
        if (events == null || events.isEmpty()) {
            return;
        }

        try {
            BulkRequest bulkRequest = new BulkRequest();
            bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE);
            for (EventEntity event : events) {
                Map<String, Object> source = objectMapper.convertValue(event, Map.class);
                IndexRequest indexRequest = new IndexRequest(INDEX_NAME).source(source);
                bulkRequest.add(indexRequest);
            }
            BulkResponse response = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (response.hasFailures()) {
                logger.error("Bulk indexing had failures: {}", response.buildFailureMessage());
                throw new RuntimeException("Bulk indexing failures: " + response.buildFailureMessage());
            }
            logger.info("Successfully saved {} events to OpenSearch", events.size());
        } catch (Exception e) {
            logger.error("Error saving events to OpenSearch", e);
            throw new RuntimeException("Failed to save events to OpenSearch", e);
        }
    }

    @Override
    public List<EventEntity> search(QueryDto query) {
        try {
            QueryBuilder qb = buildQuery(query);

            SearchSourceBuilder sourceBuilder = new SearchSourceBuilder()
                .query(qb)
                .from(query.getPage() != null ? query.getPage() * (query.getSize() != null ? query.getSize() : 20) : 0)
                .size(query.getSize() != null ? query.getSize() : 20);

            String sortField = query.getSortBy() != null ? query.getSortBy() : "timestamp";
            SortOrder order = "asc".equalsIgnoreCase(query.getSortOrder()) ? SortOrder.ASC : SortOrder.DESC;
            sourceBuilder.sort(sortField, order);

            SearchRequest searchRequest = new SearchRequest(INDEX_NAME).source(sourceBuilder);
            SearchResponse response = client.search(searchRequest, RequestOptions.DEFAULT);

            List<EventEntity> results = new ArrayList<>();
            for (SearchHit hit : response.getHits().getHits()) {
                Map<String, Object> map = hit.getSourceAsMap();
                EventEntity entity = objectMapper.convertValue(map, EventEntity.class);
                entity.setId(hit.getId());
                results.add(entity);
            }
            return results;
        } catch (Exception e) {
            logger.error("Error searching events in OpenSearch", e);
            throw new RuntimeException("Failed to search events in OpenSearch", e);
        }
    }

    @Override
    public long count(QueryDto query) {
        try {
            QueryBuilder qb = buildQuery(query);
            CountRequest countRequest = new CountRequest(INDEX_NAME);
            countRequest.query(qb);
            CountResponse response = client.count(countRequest, RequestOptions.DEFAULT);
            return response.getCount();
        } catch (Exception e) {
            logger.error("Error counting events in OpenSearch", e);
            throw new RuntimeException("Failed to count events in OpenSearch", e);
        }
    }

    private QueryBuilder buildQuery(QueryDto query) {
        BoolQueryBuilder bool = QueryBuilders.boolQuery();

        addTerm(bool, "user", query.getUser());
        addTerm(bool, "category", query.getCategory());
        addTerm(bool, "action", query.getAction());
        addTerm(bool, "document", query.getDocument());
        addTerm(bool, "project", query.getProject());
        addTerm(bool, "environment", query.getEnvironment());
        addTerm(bool, "tenant", query.getTenant());
        addTerm(bool, "correlationId", query.getCorrelationId());
        addTerm(bool, "traceId", query.getTraceId());

        Instant start = query.getStartDate();
        Instant end = query.getEndDate();
        if (start != null || end != null) {
            RangeQueryBuilder range = QueryBuilders.rangeQuery("timestamp");
            if (start != null) range.gte(start.toString());
            if (end != null) range.lte(end.toString());
            bool.filter(range);
        }

        if (bool.must().isEmpty() && bool.filter().isEmpty()) {
            return QueryBuilders.matchAllQuery();
        }
        return bool;
    }

    private void addTerm(BoolQueryBuilder bool, String field, String value) {
        if (value != null && !value.isEmpty()) {
            bool.filter(QueryBuilders.termQuery(field, value));
        }
    }
}

