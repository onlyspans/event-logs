#!/bin/bash

# Script to send events to Kafka topic for testing

TOPIC="event-logs"

echo "Sending test event to Kafka topic: $TOPIC"

# Create JSON event (single line for Kafka)
JSON_EVENT="{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"user\":\"test-user\",\"category\":\"USER_ACTION\",\"action\":\"LOGIN\",\"documentName\":\"test-document\",\"project\":\"test-project\",\"environment\":\"development\",\"tenant\":\"test-tenant\",\"correlationId\":\"corr-$(date +%s)\",\"traceId\":\"trace-$(date +%s)\",\"details\":{\"ipAddress\":\"192.168.1.100\",\"userAgent\":\"TestScript/1.0\",\"additionalInfo\":\"Automated test event\"}}"

echo "$JSON_EVENT" | docker compose exec -T kafka kafka-console-producer \
  --bootstrap-server localhost:29092 \
  --topic $TOPIC

echo "Event sent successfully!"
