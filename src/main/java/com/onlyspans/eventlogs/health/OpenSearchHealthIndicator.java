package com.onlyspans.eventlogs.health;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.onlyspans.eventlogs.entity.EventEntity;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class OpenSearchHealthIndicator {

    private final ElasticsearchOperations elasticsearchOperations;

    public OpenSearchHealthIndicator(ElasticsearchOperations elasticsearchOperations) {
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @GetMapping("/opensearch")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        try {
            // Try to get cluster info by checking if we can access the index operations
            boolean exists = elasticsearchOperations.indexOps(EventEntity.class).exists();
            health.put("status", "UP");
            health.put("opensearch", "connected");
            health.put("indexExists", exists);
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("opensearch", "disconnected");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}
