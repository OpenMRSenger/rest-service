# Failure Mode and Effects Analysis (FMEA) — OpenMRSenger Rest-Service

Failure Mode and Effects Analysis for the path:
`POST /api/webhooks/appointments` -> outbox -> RabbitMQ (`appointment.events`)
-> provider adapter (SwiftSend/AsyncFlow/SecurePost/LegacyLink) -> retry queues
(`.retry.10s/60s/600s`) -> DLQ (`appointment.events.dlq`).

### Scoring Methodology
- **Severity (S)**: 1 (Minimal impact) to 5 (System failure / security breach / GDPR compliance violation).
- **Likelihood (L)**: 1 (Very unlikely) to 5 (Frequent/inevitable under load).
- **Detection (D)**: 1 (Immediately detected/prevented) to 5 (Undetected until user incident).
- **Risk Priority Number (RPN)**: S × L × D (Max 125).

---

## FMEA Matrix

| # | Component | Failure Mode | Effect | Cause | Current Control / Code Mitigation | S | L | D | RPN | Recommended Action / Status |
|---|-----------|--------------|--------|-------|----------------------------------|---|---|---|-----|-----------------------------|
| 1 | Webhook endpoint | Invalid/missing `Authorization` header | Request rejected (401), no outbox write. | Misconfigured client or rotated token. | [ApiKeyWebhookAuthenticator](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/appointments/infrastructure/web/ApiKeyWebhookAuthenticator.java) exact-match check + warn logs. | 2 | 2 | 1 | **4** | Controlled. Review key rotation procedures. |
| 2 | Webhook endpoint | High concurrent request volume (spike) | Tomcat thread pool exhaustion, latency spikes, request timeouts. | Burst of appointment updates under peak load (e.g., clinic opening). | Default Tomcat limits (`server.tomcat.threads.max=200`). | 4 | 4 | 2 | **32** | SLA breached during load test (p95 = 2.46s). Increase Tomcat max threads or configure horizontal scaling. |
| 3 | Webhook endpoint | Malformed FHIR payload | Request rejected (400) with OperationOutcome, no outbox write. | Client sends invalid FHIR format or missing properties. | [FhirAppointmentValidator](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/appointments/infrastructure/web/fhir/FhirAppointmentValidator.java) validates resourceType, status, start time, and participants. | 2 | 2 | 1 | **4** | Controlled. |
| 4 | Webhook endpoint | Confidential credentials in request headers | Exposed client secrets in proxy, gateway, or application logs. | Accepting credentials via `x-provider-config` header. | Log masking in [LogSanitizer](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/shared/logging/LogSanitizer.java). | 4 | 3 | 3 | **36** | Store provider configuration on the server side mapped to a hospital tenant ID instead of headers. |
| 5 | Webhook endpoint | Timing attack on webhook authentication | Attacker reconstructs the API key byte-by-byte. | Direct string equals check which fails-fast on character mismatch. | Standard equals comparison in [ApiKeyWebhookAuthenticator](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/appointments/infrastructure/web/ApiKeyWebhookAuthenticator.java). | 4 | 2 | 4 | **32** | Refactor to use `MessageDigest.isEqual()` for constant-time byte-array comparison. |
| 6 | Database | Database connection loss during webhook receipt | Request fails (500), transaction rolled back, client must retry. | PostgreSQL database instance down or network outage. | `@Transactional` annotation on [AppointmentServiceImpl](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/appointments/application/AppointmentServiceImpl.java) `processWebhook()`. | 4 | 2 | 2 | **16** | Controlled. Relies on OpenMRS module retry policy to deliver webhooks later. |
| 7 | Database | Database connection loss during outbox relay | Relay loop aborted, transaction rolls back, potential duplicate delivery. | PostgreSQL connection drops midway through scheduler loop. | `@Transactional` on [RabbitMqOutboxRelay](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/appointments/infrastructure/messaging/RabbitMqOutboxRelay.java) `processOutbox()`. | 4 | 2 | 2 | **16** | If connection fails post-RabbitMQ publish but pre-DB commit, duplicate sends occur. Implement consumer deduplication. |
| 8 | Database | Lock contention / slow outbox writes | Increased commit latency, rising rollback rate. | Long-running transactions or concurrent outbox writes. | Grafana transaction monitoring and JPA isolation levels. | 3 | 2 | 2 | **12** | Controlled. |
| 9 | Database | Connection pool (HikariCP) exhaustion | Outbox writes blocked, request latency spikes. | Peak concurrent request rate exceeds pool capacity. | `hikaricp_connections_active` metrics exposed to Prometheus/Grafana. | 4 | 3 | 2 | **24** | Increase `spring.datasource.hikari.maximum-pool-size` under high traffic load. |
| 10 | Database | Missing database index on outbox event ID | Full table scans for outbox checks, high DB CPU usage. | No index defined on `event_id` in Flyway schema `V1__Initial_Schema.sql`. | JPA repository queries by event ID. | 3 | 4 | 2 | **24** | Create a database index on the `event_id` column of the `outbox_messages` table in a new migration. |
| 11 | Message Broker | Uncaught publishing error during retry scheduling | Infinite loop of immediate redeliveries, consumer thread CPU spike. | RabbitMQ broker connection fails during `eventRetryService.scheduleRetry()`. | Default Spring AMQP listener container exception handling. | 4 | 2 | 2 | **16** | Add retry container limits or catch publishing exceptions during the listener's fallback path. |
| 12 | Message Broker | Queue backlog growth under sustained failure | Delayed notifications, broker memory pressure. | Downstream provider outage + high incoming webhook rates. | Grafana RabbitMQ Queue Size dashboard panel monitoring. | 4 | 2 | 3 | **24** | Configure automatic alerts for queue depth exceeding thresholds. |
| 13 | Message Broker | Broker connection / channel exhaustion | Publish failures, silently dropped event payloads. | High concurrent publisher threads without connection pooling tuning. | Spring AMQP default connection factory caching. | 4 | 2 | 3 | **24** | Tune channel/connection cache limits in `CachingConnectionFactory`. |
| 14 | Provider Adapter | Downstream provider unavailable / rate-limited | Delivery fails, event gets routed to retry queues. | Provider offline or rate limit hit (fakecomworld limits SwiftSend/AsyncFlow). | Exception handler in [AbstractRestMessagingAdapter](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/communications/infrastructure/providers/AbstractRestMessagingAdapter.java) throws typed exception to trigger retry. | 4 | 3 | 2 | **24** | Controlled. Messages rescheduled automatically. |
| 15 | Provider Adapter | Retry stages exhausted (stages > 3) | Message routed to DLQ, notification never delivered. | Sustained provider outage longer than 11.5 minutes total backoff. | [RabbitMqEventRetryService](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/communications/infrastructure/messaging/RabbitMqEventRetryService.java) routes to DLQ after stage 3. | 5 | 2 | 2 | **20** | Controlled. Alert administrators when messages enter the DLQ. |
| 16 | Provider Adapter | Synchronous status polling blocks thread | Starvation of RabbitMQ listener pool, queue backlogs. | `Thread.sleep(5000)` inside [AsyncFlowAdapter](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/communications/infrastructure/providers/AsyncFlowAdapter.java) `pollStatus()`. | None. Runs synchronously on listener thread. | 5 | 3 | 2 | **30** | Refactor polling to use asynchronous callback or a scheduled job. |
| 17 | Provider Adapter | Multi-tenancy token collision | Authentication failure or cross-tenant data leakage. | Shared mutable fields (`accessToken`, `expiryTime`) in singleton [SecurePostAdapter](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/communications/infrastructure/providers/SecurePostAdapter.java). | Synchronized blocks for authentication. | 5 | 2 | 3 | **30** | Store tokens in a concurrent cache keyed by client credentials, avoiding mutable singleton fields. |
| 18 | Provider Adapter | Immediate notifications crash | Lookup exception during consumption, events routed to DLQ. | [AppointmentServiceImpl](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/appointments/application/AppointmentServiceImpl.java) `sendImmediately()` passes `null` event ID, violating DB primary key constraint. | Catch block in [NotificationEventListener](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/communications/application/NotificationEventListener.java) logs parsing errors. | 4 | 3 | 2 | **24** | Generate a unique random UUID for immediate notifications instead of passing `null`. |
| 19 | GDPR Scheduler | Unprocessed outbox messages deleted | Silent loss of unsent notifications. | [DataRetentionScheduler](file:///E:/School/Avans/Jaar%202_ICT/2.4/repos/rest-service/src/main/java/openmrsenger/restservice/retention/DataRetentionScheduler.java) deletes outbox records older than 14 days without checking status. | Purging query deletes by date cutoff. | 4 | 2 | 3 | **24** | Modify deletion query to check for `processed = true` or `cancelled = true`. |
| 20 | Platform | JVM Heap pressure / GC pauses during soak | Latency spikes, JVM OutOfMemoryError. | Memory leak or high payload retention in memory under long soak run. | Grafana JVM memory heap and CPU trends panels. | 4 | 1 | 2 | **8** | Controlled. Heap remained stable during the 10-minute soak test. |

---

## Fault Injection Tooling

- **Mock Service Failure**: The stress test `monitoring/stress/webhook-load-test.js` is configured to inject mock downstream service failures. By sending an empty provider configuration (`x-provider-config: {}`) for a tunable fraction of requests (default 10%), it triggers a `MessagingProviderException` in the rest-service, forcing the messages to exercise the full RabbitMQ retry queue pipeline (`.retry.10s` -> `.retry.60s` -> `.retry.600s` -> `.dlq`).

---

## Stress Test Findings

Findings logged after running `monitoring/stress/webhook-load-test.js` against the Docker Compose stack:

| Row # | Observed during test | Triggered? (Y/N) | Notes / Observations |
|-------|----------------------|-------------------|----------------------|
| 2     | Spike load p95/p99 latency | Y | Under 300 rps spike phase, p95 reached `2465.56 ms` and p99 reached `3003.28 ms` (breaching the 1s SLA threshold). No HTTP errors occurred (all requests accepted). |
| 9     | Peak active DB connections | Y | HikariCP active connections peaked during the 300 rps spike phase, but remained stable without pool exhaustion. |
| 14    | Downstream provider rate limits | Y | Simulated rate limits successfully generated `4199` failure injected requests, which were correctly routed to RabbitMQ retry queues. |
| 15    | DLQ message count | Y | Retries exhausted for injected-failure events were successfully pushed to `appointment.events.dlq` for manual auditing. |
| 16    | Synchronous polling block | Y | Thread dump analysis under peak load showed RabbitMQ listener threads blocked inside `Thread.sleep` during `AsyncFlowAdapter` polling, degrading consumer throughput. |
| 20    | JVM heap + CPU trend | Y | Memory usage remained stable over the 10-minute soak test (30 rps) with normal GC cycles and no memory leak symptoms. |
