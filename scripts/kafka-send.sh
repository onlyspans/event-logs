#!/bin/bash

# Kafka connection settings
HOST="kafka.onlyspans.ru:9092"
TOPIC="event-logs"
USER="event-logs"
PASS="${KAFKA_PASSWORD}"

# Check if KAFKA_PASSWORD is set
if [ -z "$PASS" ]; then
    echo "Error: KAFKA_PASSWORD environment variable is not set"
    echo "Usage: KAFKA_PASSWORD='your-password' $0"
    exit 1
fi

echo "Sending test event to Kafka topic: $TOPIC"

# Create JSON event (single line for Kafka)
JSON_EVENT="{\"timestamp\":\"$(date -u +%Y-%m-%dT%H:%M:%SZ)\",\"user\":\"test-user\",\"category\":\"USER_ACTION\",\"action\":\"LOGIN\",\"documentName\":\"test-document\",\"project\":\"test-project\",\"environment\":\"development\",\"tenant\":\"test-tenant\",\"correlationId\":\"corr-$(date +%s)\",\"traceId\":\"trace-$(date +%s)\",\"details\":{\"ipAddress\":\"192.168.1.100\",\"userAgent\":\"TestScript/1.0\",\"additionalInfo\":\"Automated test event\"}}"

# Send message to Kafka using kcat
echo "$JSON_EVENT" | kcat -P \
    -b "$HOST" \
    -t "$TOPIC" \
    -X security.protocol=SASL_PLAINTEXT \
    -X sasl.mechanism=SCRAM-SHA-512 \
    -X sasl.username="$USER" \
    -X sasl.password="$PASS"

if [ $? -eq 0 ]; then
    echo "Event sent successfully!"
else
    echo "Failed to send event"
    exit 1
fi
