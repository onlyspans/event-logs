package com.onlyspans.eventlogs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ReadinessController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ReadinessController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/readyz")
    public ResponseEntity<Map<String, Object>> ready() {
        Map<String, Object> health = new HashMap<>();
        try {
            // Check Kafka connectivity
            kafkaTemplate.getProducerFactory().createProducer();
            
            // If we get here, Kafka is accessible
            health.put("status", "UP");
            health.put("kafka", "connected");
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("kafka", "disconnected");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}

