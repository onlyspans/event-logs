# Kafka to OpenSearch Architecture

## Overview

This document describes the guaranteed delivery architecture for event logs from Kafka to OpenSearch.

## Architecture Principles

### At-Least-Once Delivery Guarantee

The system ensures that **no events are lost** during the flow from Kafka to OpenSearch by following these principles:

1. **Batch Processing** - Events are consumed from Kafka in batches (configurable via `MAX_POLL_RECORDS_CONFIG`)
2. **Synchronous Write** - All events in a batch are written to OpenSearch before acknowledging to Kafka
3. **Manual Acknowledgment** - Kafka offset is committed **only after** successful write to OpenSearch
4. **Automatic Retry** - Failed batches are automatically reprocessed by Kafka (no acknowledgment = redelivery)

## Data Flow

```
┌─────────┐     Batch (N messages)      ┌──────────────────┐
│  Kafka  │ ──────────────────────────> │ KafkaEventConsumer│
│         │                              │                  │
└─────────┘                              └────────┬─────────┘
     ↑                                            │
     │                                            │ Parse messages
     │                                            │ Convert to entities
     │                                            ↓
     │                                   ┌─────────────────┐
     │                                   │  IEventStorage  │
     │                                   │  (OpenSearch)   │
     │                                   └────────┬────────┘
     │                                            │
     │                                            │ Bulk Insert
     │                                            ↓
     │                                   ┌─────────────────┐
     │                                   │   OpenSearch    │
     │                                   └────────┬────────┘
     │                                            │
     │                                            │ Success?
     │                                            ↓
     │                                          [Yes]
     │                                            │
     └────────────────────────────────────────────┘
              Acknowledge (commit offset)
```

## Critical Guarantees

### ✅ No Data Loss Scenarios

1. **Application Restart** - No in-memory queue; events remain in Kafka until acknowledged
2. **OpenSearch Failure** - Batch is not acknowledged; Kafka redelivers after consumer timeout
3. **Partial Batch Failure** - Individual message parsing errors are logged; valid messages are saved
4. **Network Issues** - Transaction-like behavior: all or nothing acknowledgment

### ⚠️ Important Considerations

**Possible Duplicates**: In case of failure between OpenSearch write and Kafka acknowledgment, events may be redelivered. This is acceptable for logging systems.

**Idempotency**: OpenSearch uses document IDs from events, so redelivery of the same event updates the same document (natural idempotency).

## Configuration

### Kafka Consumer Settings

Located in `KafkaConfig.java` and `application.properties`:

```properties
# Batch size - maximum records in one poll (not minimum!)
kafka.consumer.max-poll-records=100

# Minimum bytes to fetch (1 = immediate response, higher = wait for more data)
kafka.consumer.fetch-min-bytes=1

# Max wait time if min bytes not reached (milliseconds)
kafka.consumer.fetch-max-wait-ms=500
```

### How Batching Works

**Important**: `max-poll-records` is a **maximum**, not a minimum!

#### Scenario 1: Low Load (1 message in topic)
```
Time: 0ms  - Consumer polls Kafka
Time: 10ms - Kafka responds immediately with 1 message
Time: 20ms - Message processed and written to OpenSearch
Time: 30ms - Acknowledged

Total latency: ~30ms
```

#### Scenario 2: Medium Load (50 messages in topic)
```
Time: 0ms  - Consumer polls Kafka
Time: 10ms - Kafka responds with 50 messages
Time: 100ms - All 50 processed and written to OpenSearch (bulk)
Time: 110ms - Acknowledged

Total latency: ~110ms
```

#### Scenario 3: High Load (1000+ messages in topic)
```
Poll 1: 100 messages → process → acknowledge (200ms)
Poll 2: 100 messages → process → acknowledge (200ms)
Poll 3: 100 messages → process → acknowledge (200ms)
... continuous processing

Throughput: ~500 messages/second
```

#### Scenario 4: No Messages
```
Time: 0ms  - Consumer polls Kafka
Time: 500ms - Kafka waits up to fetch-max-wait-ms
Time: 500ms - Returns empty batch
Time: 510ms - Empty batch acknowledged

Consumer keeps polling in a loop
```

### Tuning Parameters

| Parameter | Default | Effect | When to Increase | When to Decrease |
|-----------|---------|--------|------------------|------------------|
| `max-poll-records` | 100 | Max batch size | Higher throughput needed, more memory available | Lower memory usage, lower latency |
| `fetch-min-bytes` | 1 | Min data to fetch | Want better batching efficiency | Want lower latency for single messages |
| `fetch-max-wait-ms` | 500 | Max wait time | Want better batching under low load | Want lower latency, fast response time |
| `session-timeout-ms` | 30000 | Consumer timeout | OpenSearch writes are slow | Want faster failure detection |
| `heartbeat-interval-ms` | 10000 | Heartbeat frequency | Never (must be < session-timeout/3) | Want more responsive health checks |

### Latency vs Throughput Trade-offs

#### Low Latency Configuration (for real-time processing)
```properties
kafka.consumer.max-poll-records=50
kafka.consumer.fetch-min-bytes=1
kafka.consumer.fetch-max-wait-ms=100
```
**Result**: Single messages processed in ~100ms, good for low-traffic systems

#### High Throughput Configuration (for batch processing)
```properties
kafka.consumer.max-poll-records=500
kafka.consumer.fetch-min-bytes=10000
kafka.consumer.fetch-max-wait-ms=2000
```
**Result**: Waits for more data, processes larger batches, higher throughput

## Error Handling

### Parsing Errors

If individual messages fail to parse:
- Error is logged with full message content
- `eventsFailedCounter` metric is incremented
- Valid messages in the batch are still processed
- If **all** messages fail to parse, batch is **not acknowledged** (will be redelivered)

### OpenSearch Errors

If bulk insert to OpenSearch fails:
- Error is logged
- Batch is **not acknowledged**
- Kafka will redeliver the entire batch after consumer timeout
- Application continues processing new batches (failure is isolated)

### Fatal Errors

For unrecoverable errors (e.g., OpenSearch down):
- Consumer will eventually be marked as dead by Kafka
- Kafka will reassign partitions to other consumers (if available)
- Events remain in Kafka until successfully processed

## Monitoring

### Metrics

The following Prometheus metrics are exposed:

```
# Events received from Kafka
event_logs_kafka_received_total

# Batches successfully processed
event_logs_kafka_batches_processed_total

# Individual events that failed to parse
event_logs_kafka_failed_total
```

### Health Checks

Monitor these indicators:

1. **Consumer Lag** - Use Kafka monitoring tools to check consumer lag
2. **Processing Rate** - `batches_processed_total` should increase steadily
3. **Failure Rate** - `failed_total` should be zero or minimal
4. **OpenSearch Health** - Monitor via `/actuator/health` endpoint

## Comparison: Old vs New Architecture

### Old Architecture (Removed)

```
Kafka → In-Memory Queue → Job (polling every 5s) → OpenSearch
         ↓ acknowledge()
```

**Problems:**
- ❌ Data loss on restart (in-memory queue)
- ❌ Race condition between acknowledge and OpenSearch write
- ❌ Unnecessary polling job
- ❌ Extra abstraction layer (IEventInput)

### New Architecture (Current)

```
Kafka → Batch Consumer → OpenSearch → acknowledge()
```

**Benefits:**
- ✅ No data loss (acknowledge only after successful write)
- ✅ Better performance (batch processing)
- ✅ Simpler architecture (fewer components)
- ✅ Lower latency (no polling delay)

## Disaster Recovery

### Kafka Retention

Ensure Kafka topic retention is configured appropriately:

```properties
# Kafka topic configuration (set on broker/topic level)
retention.ms=604800000  # 7 days
retention.bytes=-1      # Unlimited
```

### Recovery Scenarios

1. **OpenSearch data loss**: Reset consumer group offset to replay events from Kafka
2. **Consumer crash**: Other consumers in group take over partitions
3. **Kafka cluster failure**: Events are replicated across brokers

## Future Improvements

### Dead Letter Queue (DLQ)

For messages that consistently fail parsing:
- Configure DLQ topic in Kafka
- Send unparseable messages to DLQ after N retries
- Manual inspection and reprocessing

### Exactly-Once Semantics

To eliminate duplicates:
- Implement Kafka transactions
- Use OpenSearch optimistic locking
- Add deduplication layer

### Performance Optimization

- Increase `MAX_POLL_RECORDS_CONFIG` for higher throughput
- Enable OpenSearch bulk request optimization
- Add async processing with CompletableFuture
