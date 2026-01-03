package com.onlyspans.eventlogs.health;

import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.GetIndexRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@ConditionalOnProperty(name = "storage.type", havingValue = "opensearch")
public class OpenSearchHealthIndicator {

    private final RestHighLevelClient client;

    public OpenSearchHealthIndicator(RestHighLevelClient client) {
        this.client = client;
    }

    @GetMapping("/opensearch")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        try {
            boolean ping = client.ping(RequestOptions.DEFAULT);
            boolean exists = false;
            try {
                exists = client.indices().exists(new GetIndexRequest("event-logs"), RequestOptions.DEFAULT);
            } catch (Exception ignored) {}
            health.put("status", ping ? "UP" : "DOWN");
            health.put("opensearch", ping ? "connected" : "disconnected");
            health.put("indexExists", exists);
            return ping ? ResponseEntity.ok(health) : ResponseEntity.status(503).body(health);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("opensearch", "disconnected");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}
