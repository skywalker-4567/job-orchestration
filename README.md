# Global Job Orchestration Platform

A production-grade distributed job execution system built with Spring Boot, Apache Kafka, PostgreSQL, and React. The platform dispatches jobs to region-specific workers, tracks execution lifecycle, handles retries, detects timeouts, and exposes a live monitoring dashboard.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [System Design Decisions](#system-design-decisions)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Kafka Topics](#kafka-topics)
- [Database Schema](#database-schema)
- [Execution Lifecycle](#execution-lifecycle)
- [Retry and Failure Handling](#retry-and-failure-handling)
- [Outbox Pattern](#outbox-pattern)
- [Timeout Detection (Reaper)](#timeout-detection-reaper)
- [Dead Letter Queue](#dead-letter-queue)
- [Observability](#observability)
- [Dashboard](#dashboard)
- [Testing](#testing)
- [Known Limitations and Future Work](#known-limitations-and-future-work)

---

## Overview

This platform solves the problem of reliable distributed job execution across multiple geographic regions. A client submits a job via REST API. The control plane persists it, assigns it to a region using round-robin scheduling, and dispatches it to a Kafka topic. A worker in that region picks it up, executes it, and reports the outcome back to the control plane. The control plane derives job status from execution outcomes, handles retries up to a configurable maximum, and routes permanently failed jobs to a Dead Letter Queue.

The system is designed around the following principles:

- **Separation of concerns** — the control plane owns all state; workers are stateless
- **At-least-once delivery** — Kafka guarantees delivery; idempotency guards against duplicate processing
- **Reliability over speed** — the Outbox pattern decouples DB commits from Kafka publishes
- **Explicit failure** — timeouts are detected and surfaced, not silently ignored

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          CLIENT / DASHBOARD                         │
│                     POST /jobs    GET /metrics                      │
└─────────────────────────────┬───────────────────────────────────────┘
                              │ REST
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        CONTROL PLANE                                │
│                                                                     │
│   JobService          ExecutionService        ReaperService         │
│   JobController       ExecutionController     MetricsService        │
│   OutboxPublisher     GlobalExceptionHandler                        │
│                                                                     │
│   ┌──────────────┐   ┌──────────────┐   ┌──────────────────────┐   │
│   │  PostgreSQL  │   │ Outbox Table │   │  Spring Scheduler    │   │
│   │  job         │   │ (unpublished)│   │  (Reaper + Outbox)   │   │
│   │  job_exec    │   └──────┬───────┘   └──────────────────────┘   │
│   └──────────────┘          │                                       │
└─────────────────────────────┼───────────────────────────────────────┘
                              │ Kafka Publish (after DB commit)
                              ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        APACHE KAFKA                                 │
│                                                                     │
│   jobs.US_EAST      jobs.EU_WEST      jobs.AP_SOUTH    jobs.DLQ    │
└──────────┬────────────────┬────────────────┬────────────────────────┘
           │                │                │
           ▼                ▼                ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ worker       │  │ worker       │  │ worker       │
│ US_EAST      │  │ EU_WEST      │  │ AP_SOUTH     │
│              │  │              │  │              │
│ consume →    │  │ consume →    │  │ consume →    │
│ simulate →   │  │ simulate →   │  │ simulate →   │
│ POST /start  │  │ POST /start  │  │ POST /start  │
│ POST /complete│  │ POST /complete│  │ POST /complete│
└──────────────┘  └──────────────┘  └──────────────┘
```

---

## Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Control Plane | Spring Boot | 3.2.5 |
| Persistence | Spring Data JPA + Hibernate | 6.x |
| Database | PostgreSQL | 15 |
| Message Broker | Apache Kafka | 7.5.0 (Confluent) |
| Coordination | Apache Zookeeper | 7.5.0 (Confluent) |
| Workers | Spring Boot | 3.2.5 |
| Dashboard | React + Vite | React 18, Vite 5 |
| HTTP Client (Worker) | Spring RestTemplate | — |
| Serialization | Jackson + JSR310 | — |
| Validation | Jakarta Bean Validation | — |
| Containerization | Docker + Docker Compose | — |
| Java | Eclipse Temurin | 21 |
| Node | Alpine | 20 |

---

## Project Structure

```
job-orchestration/
├── docker-compose.yml
│
├── control-plane/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/jobplatform/controlplane/
│       ├── ControlPlaneApplication.java
│       ├── assignment/
│       │   └── RegionAssignmentService.java       # Round-robin region selector
│       ├── config/
│       │   ├── CorsConfig.java                    # CORS for dashboard
│       │   ├── JacksonConfig.java                 # ObjectMapper with JSR310
│       │   └── KafkaProducerConfig.java           # Kafka producer factory
│       ├── controller/
│       │   ├── ExecutionController.java           # /executions endpoints
│       │   ├── JobController.java                 # /jobs endpoints
│       │   └── MetricsController.java             # /metrics endpoints
│       ├── dto/
│       │   ├── ErrorResponse.java
│       │   ├── ExecutionMetricsResponse.java
│       │   ├── FailExecutionRequest.java
│       │   ├── JobExecutionResponse.java
│       │   ├── JobMetricsResponse.java
│       │   ├── JobRequest.java                    # @Valid annotated
│       │   ├── JobResponse.java
│       │   └── StartExecutionRequest.java
│       ├── entity/
│       │   ├── Job.java                           # @Version for optimistic locking
│       │   ├── JobExecution.java                  # @Version for optimistic locking
│       │   └── OutboxEvent.java                   # Outbox pattern table
│       ├── enums/
│       │   ├── ExecutionStatus.java
│       │   ├── JobStatus.java
│       │   ├── Priority.java
│       │   └── Region.java
│       ├── exception/
│       │   └── GlobalExceptionHandler.java        # @RestControllerAdvice
│       ├── producer/
│       │   ├── DLQProducer.java
│       │   ├── JobDLQMessage.java
│       │   ├── JobExecutionMessage.java
│       │   ├── JobProducer.java
│       │   ├── KafkaDLQProducer.java
│       │   └── KafkaJobProducer.java
│       ├── repository/
│       │   ├── JobExecutionRepository.java
│       │   ├── JobRepository.java
│       │   └── OutboxEventRepository.java
│       └── service/
│           ├── ExecutionService.java              # Core lifecycle logic
│           ├── JobService.java                    # Job creation + queries
│           ├── MetricsService.java                # Aggregate metrics
│           ├── OutboxPublisher.java               # Scheduled Kafka publisher
│           └── ReaperService.java                 # Scheduled timeout detector
│   └── src/main/resources/
│       └── application.properties
│
├── worker/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/jobplatform/worker/
│       ├── WorkerApplication.java
│       ├── client/
│       │   └── ControlPlaneClient.java            # RestTemplate wrapper
│       ├── config/
│       │   ├── AppConfig.java                     # RestTemplate bean
│       │   └── KafkaConsumerConfig.java           # Consumer factory
│       ├── consumer/
│       │   └── JobExecutionConsumer.java          # @KafkaListener
│       ├── model/
│       │   └── JobExecutionMessage.java
│       └── service/
│           ├── IdempotencyService.java            # In-memory dedup store
│           └── WorkerExecutionService.java        # Execution simulation
│   └── src/main/resources/
│       └── application.properties
│
└── dashboard/
    ├── Dockerfile
    ├── vite.config.js                             # Proxy to control-plane
    ├── package.json
    ├── index.html
    └── src/
        ├── App.jsx
        ├── main.jsx
        ├── components/
        │   └── JobList.jsx                        # Expandable job table
        ├── pages/
        │   └── Dashboard.jsx                      # Metrics + job list
        └── services/
            └── api.js                             # Axios API layer
```

---

## System Design Decisions

### Control Plane Owns All State

Workers are intentionally stateless. They have no database connection. All state transitions — job status, execution status, attempt tracking — are performed exclusively by the control plane. Workers call the control plane's REST APIs to report outcomes. This ensures a single source of truth and makes workers horizontally scalable and replaceable.

### Outbox Pattern for Kafka Reliability

A naive implementation publishes directly to Kafka inside the job creation transaction. If Kafka is unavailable or the application crashes after the DB commit but before the publish, the message is lost and the job stays `SUBMITTED` forever with no execution.

The Outbox pattern solves this by writing a pending message record to an `outbox_event` table inside the same DB transaction as the job and execution. A separate scheduled process reads unpublished records and publishes them to Kafka, marking them published only on success. If Kafka is down, the record remains unpublished and will be retried on the next scheduler cycle.

### Round-Robin Region Assignment

Region assignment uses an `AtomicInteger` counter with modulo arithmetic. This is thread-safe without locks and produces a deterministic, fair distribution across `US_EAST`, `EU_WEST`, and `AP_SOUTH` in sequence. No external coordination is required.

### Optimistic Locking

Both `Job` and `JobExecution` entities carry a `@Version` field managed by Hibernate. If two threads attempt to update the same record concurrently, one will receive an `OptimisticLockingFailureException`, which is caught and converted to a `409 Conflict` response by the global exception handler. This prevents silent data corruption without the overhead of pessimistic locks.

### Worker Idempotency

Kafka delivers messages at-least-once. A worker may receive the same message twice in failure recovery scenarios. Each message carries a unique `eventId`. The `IdempotencyService` maintains an in-memory `ConcurrentHashMap` set of processed event IDs. Before processing, the worker checks this set and skips duplicates. The `eventId` is marked processed only after the execution outcome is reported to the control plane.

---

## Getting Started

### Prerequisites

- Docker Desktop installed and running
- Java 21 and Maven (for building the JARs)
- Node.js 20+ (only if running dashboard locally outside Docker)

### Build

```bash
# Build control plane JAR
cd control-plane
mvn clean package -DskipTests
cd ..

# Build worker JAR
cd worker
mvn clean package -DskipTests
cd ..
```

### Run

```bash
docker-compose up --build
```

This starts 8 containers in dependency order:

1. `postgres` — waits for `pg_isready`
2. `zookeeper` — waits for port readiness
3. `kafka` — waits for broker API availability
4. `control-plane` — waits for `/actuator/health`
5. `worker-us-east`, `worker-eu-west`, `worker-ap-south` — wait for control-plane health
6. `dashboard` — waits for control-plane health

Allow approximately 90 seconds for all services to reach healthy state on first boot.

### Verify

```bash
docker-compose ps
```

All 8 containers should show `Up`. Then visit:

- **Dashboard** — http://localhost:5173
- **Control Plane API** — http://localhost:8080
- **Health Check** — http://localhost:8080/actuator/health

---

## Configuration

### Control Plane (`application.properties`)

```properties
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/jobplatform}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:postgres}

spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false

spring.kafka.bootstrap-servers=${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}

management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
```

### Worker (`application.properties`)

```properties
spring.kafka.bootstrap-servers=${SPRING_KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=worker-group

worker.region=${WORKER_REGION:US_EAST}
control-plane.base-url=${CONTROL_PLANE_BASE_URL:http://localhost:8080}
```

All sensitive values are environment-variable driven with local defaults for development.

---

## API Reference

### Jobs

#### `POST /jobs`

Submit a new job for execution.

**Request body:**
```json
{
  "taskType": "IMAGE_RESIZE",
  "payload": { "imageUrl": "https://example.com/img.png", "width": 800 },
  "priority": "HIGH",
  "maxAttempts": 3,
  "timeoutSeconds": 300
}
```

| Field | Type | Required | Notes |
|---|---|---|---|
| taskType | String | Yes | Any non-blank string |
| payload | Object | Yes | Arbitrary JSON, stored as JSONB |
| priority | Enum | Yes | `HIGH`, `MEDIUM`, `LOW` |
| maxAttempts | Integer | No | Default 3, minimum 1 |
| timeoutSeconds | Integer | No | Null means no timeout |

**Response `201 Created`:**
```json
{
  "jobId": "uuid",
  "status": "SUBMITTED",
  "createdAt": "2026-04-24T05:00:00Z"
}
```

---

#### `GET /jobs`

Returns all jobs ordered by creation time (newest first).

---

#### `GET /jobs/{jobId}`

Returns a single job by ID. Returns `404` if not found.

---

#### `GET /jobs/{jobId}/executions`

Returns all execution attempts for a job, including attempt number, status, region, worker ID, timestamps, and error messages.

---

### Executions

#### `POST /executions/{executionId}/start`

Called by a worker when it begins processing. Transitions execution from `ASSIGNED` to `RUNNING`.

```json
{ "workerId": "worker-US_EAST-abc123" }
```

Returns `409` if execution is not in `ASSIGNED` state. Returns `400` if `workerId` is blank.

---

#### `POST /executions/{executionId}/complete`

Called by a worker on success. Transitions execution to `COMPLETED` and job to `COMPLETED`.

```json
{}
```

Returns `409` if execution is not in `RUNNING` state.

---

#### `POST /executions/{executionId}/fail`

Called by a worker on failure. Transitions execution to `FAILED`. Triggers retry if attempts remain, or marks job `FAILED` and publishes to DLQ if exhausted.

```json
{ "errorMessage": "Connection timeout after 30s" }
```

---

### Metrics

#### `GET /metrics/jobs`

```json
{
  "totalJobs": 42,
  "completedJobs": 35,
  "failedJobs": 4,
  "inProgressJobs": 3
}
```

#### `GET /metrics/executions`

```json
{
  "totalExecutions": 51,
  "runningExecutions": 3,
  "completedExecutions": 35,
  "failedExecutions": 10,
  "timeoutExecutions": 3,
  "avgExecutionTimeMs": 1842.5
}
```

---

### Error Responses

All errors return a consistent JSON structure:

```json
{
  "timestamp": "2026-04-24T05:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "taskType: taskType is required",
  "path": "/jobs"
}
```

| HTTP Status | Cause |
|---|---|
| 400 | Validation failure or illegal argument |
| 404 | Resource not found |
| 409 | Invalid state transition or concurrent update detected |
| 500 | Unexpected server error |

---

## Kafka Topics

| Topic | Publisher | Consumer | Purpose |
|---|---|---|---|
| `jobs.US_EAST` | OutboxPublisher | worker-us-east | Job dispatch to US East region |
| `jobs.EU_WEST` | OutboxPublisher | worker-eu-west | Job dispatch to EU West region |
| `jobs.AP_SOUTH` | OutboxPublisher | worker-ap-south | Job dispatch to AP South region |
| `jobs.DLQ` | KafkaDLQProducer | None (future) | Permanently failed jobs |

### Message Schema (`jobs.*`)

```json
{
  "executionId": "uuid",
  "jobId": "uuid",
  "taskType": "IMAGE_RESIZE",
  "payload": {},
  "attemptNumber": 1,
  "region": "US_EAST",
  "eventId": "uuid"
}
```

`eventId` is unique per dispatch event and is used by workers for idempotency deduplication.

---

## Database Schema

### `job`

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| task_type | VARCHAR | Not null |
| payload | JSONB | Not null |
| status | VARCHAR | `SUBMITTED`, `IN_PROGRESS`, `COMPLETED`, `FAILED` |
| max_attempts | INT | Default 3 |
| priority | VARCHAR | `HIGH`, `MEDIUM`, `LOW` |
| timeout_seconds | INT | Nullable |
| created_at | TIMESTAMPTZ | Auto-set |
| updated_at | TIMESTAMPTZ | Auto-updated |
| version | BIGINT | Optimistic lock counter |

Indexes: `status`, `created_at`

---

### `job_execution`

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| job_id | UUID | FK → job(id) ON DELETE CASCADE |
| region | VARCHAR | `US_EAST`, `EU_WEST`, `AP_SOUTH` |
| status | VARCHAR | `ASSIGNED`, `RUNNING`, `COMPLETED`, `FAILED`, `TIMEOUT` |
| attempt_number | INT | Sequential per job |
| worker_id | VARCHAR | Nullable, set on start |
| event_id | UUID | Unique per dispatch |
| assigned_at | TIMESTAMPTZ | Set on creation |
| started_at | TIMESTAMPTZ | Set on start |
| completed_at | TIMESTAMPTZ | Set on complete, fail, or timeout |
| error_message | TEXT | Nullable |
| version | BIGINT | Optimistic lock counter |

Constraints: `UNIQUE(job_id, attempt_number)`, `UNIQUE(event_id)`

Indexes: `job_id`, `region`, `status`, `worker_id`

---

### `outbox_event`

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| aggregate_type | VARCHAR | Always `JOB_EXECUTION` |
| aggregate_id | UUID | executionId |
| event_type | VARCHAR | Always `EXECUTION_ASSIGNED` |
| payload | TEXT | JSON-serialized `JobExecutionMessage` |
| created_at | TIMESTAMPTZ | Auto-set |
| published | BOOLEAN | False until successfully sent to Kafka |

Index: `published` (for efficient unpublished record queries)

---

## Execution Lifecycle

```
Job submitted
      │
      ▼
  SUBMITTED ──── JobExecution created (ASSIGNED) ──── OutboxEvent written
                                                              │
                                                    OutboxPublisher sends to Kafka
                                                              │
                                                       Worker consumes
                                                              │
                                               POST /executions/{id}/start
                                                              │
                                                    ┌─── RUNNING ────┐
                                                    │                │
                                         POST /complete        POST /fail
                                                    │                │
                                                COMPLETED         FAILED
                                                    │                │
                                             Job → COMPLETED   attempts < max?
                                                              │         │
                                                             YES        NO
                                                              │         │
                                                     New JobExecution  Job → FAILED
                                                     (next attempt)    DLQ published
```

---

## Retry and Failure Handling

When an execution fails:

1. The control plane checks the total number of execution attempts for the job against `maxAttempts`.
2. If attempts remain, a new `JobExecution` is created with `attemptNumber + 1` and status `ASSIGNED`. A new outbox event is written and the job stays `IN_PROGRESS`.
3. If all attempts are exhausted and all executions are in terminal failure state (`FAILED` or `TIMEOUT`), the job is marked `FAILED` and a DLQ message is published.

A stale execution guard prevents out-of-order failures from triggering incorrect retries. Before creating a retry, the system verifies the failing execution is the latest attempt for the job. If a delayed failure report arrives for an older attempt after a newer one has already started, it is ignored.

---

## Outbox Pattern

Direct Kafka publishing inside a database transaction creates a reliability gap: if Kafka is unavailable or the application crashes after the commit but before the send, messages are permanently lost.

The Outbox pattern eliminates this gap:

1. When a job is created or a retry is triggered, an `OutboxEvent` record is written to the database **inside the same transaction** as the job/execution changes. This is atomic — either both succeed or neither does.
2. A `@Scheduled` `OutboxPublisher` runs every 3 seconds. It fetches all `OutboxEvent` records where `published = false`, deserializes the payload, calls `jobProducer.publishJobExecution(...)`, and marks the record `published = true` only on success.
3. If Kafka is down, the event remains unpublished and will be retried automatically on the next cycle.

This guarantees that every committed job will eventually reach a worker, regardless of Kafka availability at the moment of job creation.

---

## Timeout Detection (Reaper)

The `ReaperService` runs every 5 seconds and scans all `RUNNING` executions. For each one where `timeoutSeconds` is set, it calculates:

```
execution.startedAt + timeoutSeconds < now
```

If the execution has exceeded its deadline, the reaper:

1. Sets the execution status to `TIMEOUT` and records `completedAt`.
2. Calls `failExecution()` which applies the same retry and DLQ logic as a normal failure.

The `TIMEOUT` status is preserved in the execution record and is never overwritten to `FAILED`, allowing timeouts to be distinguished from explicit worker failures in metrics and logs.

---

## Dead Letter Queue

When a job exhausts all attempts, a `JobDLQMessage` is published to the `jobs.DLQ` Kafka topic containing:

- Job ID and execution ID
- Task type and payload
- Final attempt number and region
- Error message
- Timestamp

No consumer currently reads from `jobs.DLQ`. It is designed to be consumed by a future alerting, replay, or audit service. Messages in the DLQ can be inspected using any Kafka consumer or tool such as `kafka-console-consumer`.

---

## Observability

### Logging

Structured SLF4J logging is present at key points throughout the control plane and worker:

| Event | Log Message |
|---|---|
| Job created | `Job created: jobId={}, taskType={}, priority={}` |
| Execution dispatched | `Dispatching execution: executionId={}, region={}` |
| Execution started | `Execution started: executionId={}, attempt={}, workerId={}` |
| Execution completed | `Execution completed: executionId={}, attempt={}` |
| Execution failed | `Execution failed: executionId={}, attempt={}, reason={}` |
| Retry triggered | `Retrying execution: jobId={}, attempt={}` |
| Job moved to DLQ | `Job moved to DLQ: jobId={}` |
| Outbox event published | `Outbox event published: eventId={}, executionId={}` |
| Worker received message | `Received execution: executionId={}, region={}` |
| Worker success | `Execution success: executionId={}` |
| Worker failure | `Execution failed: executionId={}, error={}` |
| Control plane call failed | `Failed to start/complete/report execution: executionId={}, error={}` |

### Spring Actuator

Health and info endpoints are exposed:

```
GET /actuator/health
```

Returns status of the application, database connection, and Kafka connectivity. Used by Docker healthchecks to gate dependent service startup.

### Viewing Logs

```bash
# Control plane logs
docker-compose logs -f control-plane

# Specific worker logs
docker-compose logs -f worker-us-east

# All services
docker-compose logs -f
```

---

## Dashboard

The React dashboard runs at http://localhost:5173 and auto-refreshes every 5 seconds.

### Features

- **Job Metrics** — total, completed, failed, and in-progress job counts with color-coded cards
- **Execution Metrics** — total, running, completed, failed, and timeout execution counts with average execution time
- **Job List** — sortable table of all jobs showing ID, status, task type, priority, and creation time
- **Execution Drill-Down** — click any job row to expand inline execution details including attempt number, status, region, worker ID, error message, and duration

### Proxy Configuration

The Vite dev server proxies all API calls to the control plane:

```javascript
proxy: {
  '/jobs': 'http://control-plane:8080',
  '/executions': 'http://control-plane:8080',
  '/metrics': 'http://control-plane:8080',
  '/actuator': 'http://control-plane:8080',
}
```

Inside Docker, `control-plane` resolves to the control plane container via Docker's internal DNS. Externally, the Vite server is accessible at `localhost:5173` and the proxy handles routing.

---

## Testing

A Postman collection is provided covering the full test surface. Import `postman-collection.json` and run sections in order.

### Test Sections

| Section | What it tests |
|---|---|
| System Health | Actuator health, baseline metrics |
| Validation Errors | 400 for missing fields, 404 for unknown IDs |
| Job Submission | Three jobs auto-capturing IDs via test scripts |
| Job Queries | List all jobs, get by ID, list executions |
| Manual Execution | Full start → complete lifecycle; invalid state 409; blank workerId 400 |
| Retry Flow | Three manual failures exhausting `maxAttempts`, DLQ trigger, final `FAILED` status |
| Final Metrics | Counts and average execution time after full test run |

### Manual Testing Notes

Sections 5 and 6 require workers to be stopped so executions remain in `ASSIGNED` state long enough to be manually advanced:

```bash
# Stop workers
docker-compose stop worker-us-east worker-eu-west worker-ap-south

# Run manual test sections in Postman

# Restart workers
docker-compose start worker-us-east worker-eu-west worker-ap-south
```

---

## Known Limitations and Future Work

### Current Limitations

**Kafka publish is not fully atomic with DLQ.** The DLQ publish in `failExecution()` happens inside the database transaction. If the transaction rolls back after the DLQ message is sent, a phantom DLQ entry is created. The Outbox pattern should be extended to cover DLQ publishing as well.

**In-memory idempotency store.** The worker's `IdempotencyService` uses a `ConcurrentHashMap` that is lost on restart. After a worker restart, recently processed events could be reprocessed. A Redis-backed store with TTL would make this durable.

**No pessimistic concurrency control.** Optimistic locking detects conflicts but does not prevent them. Under very high concurrency, conflict rates could increase. Row-level locking or a `SELECT FOR UPDATE` approach would eliminate conflicts entirely.

**Full table scan for metrics.** `getExecutionMetrics()` loads all completed executions into memory to calculate the average. At scale this becomes a memory and latency issue. A native SQL aggregation query (`AVG(completed_at - started_at)`) would be far more efficient.

**Worker blocks Kafka consumer thread.** `Thread.sleep()` in `WorkerExecutionService.process()` blocks the Kafka listener thread. A thread pool executor would allow one worker container to process multiple jobs concurrently.

### Planned Improvements

- Outbox pattern extended to DLQ publishing
- Redis-backed idempotency with configurable TTL
- Prometheus metrics endpoint via Spring Actuator
- Grafana dashboard for time-series execution metrics
- Async worker execution via `@Async` and configurable thread pool
- Pagination and status filtering on `GET /jobs`
- WebSocket or Server-Sent Events for real-time dashboard updates instead of polling
- Kubernetes deployment manifests with Horizontal Pod Autoscaler for workers
- Integration test suite using Testcontainers

---

## License

This project is for educational and demonstration purposes.
