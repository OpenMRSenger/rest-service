# FMEA — Appointment Webhook & Messaging Pipeline

Failure Mode and Effects Analysis for the path:
`POST /api/webhooks/appointments` -> outbox -> RabbitMQ (`appointment.events`)
-> provider adapter (SwiftSend/AsyncFlow/SecurePost/LegacyLink) -> retry queues
(`.retry.10s/60s/600s`) -> DLQ (`appointment.events.dlq`).

Severity (S), Likelihood (L), Detection (D) scored 1 (low) - 5 (high).
RPN = S x L x D.

| # | Component | Failure Mode | Effect | Cause | Current Control | S | L | D | RPN |
|---|-----------|--------------|--------|-------|------------------|---|---|---|-----|
| 1 | Webhook endpoint | Invalid/missing `Authorization` header | Request rejected (401), no appointment lost (caller's responsibility to retry) | Misconfigured OpenMRS module / rotated secret | `ApiKeyWebhookAuthenticator` exact-match check + warn log | 2 | 2 | 1 | 4 |
| 2 | Webhook endpoint | High concurrent request volume (spike) | Increased p95/p99 latency, possible request timeouts, Tomcat thread pool exhaustion | Burst of appointment changes (e.g. batch import, clinic open) | None explicit — relies on default Spring Boot connector limits | 4 | 3 | 2 | 24 |
| 3 | Webhook endpoint | Malformed FHIR Appointment payload | Request rejected (400) with OperationOutcome, no outbox write | Caller sends invalid/incomplete FHIR resource (missing resourceType/status/start/participant) | `FhirAppointmentValidator` rejects before any processing | 2 | 2 | 1 | 4 |
| 4 | Provider adapter | Missing/malformed `x-provider-config` | `MessagingProviderException`, message routed to retry pipeline | Caller omits header, bad JSON, wrong field names | `AbstractRestMessagingAdapter` validates JSON before send; routed to `EventRetryService` | 3 | 2 | 1 | 6 |
| 5 | Provider adapter | Downstream provider (fakecomworld) unavailable, slow, or rate-limited | Request to provider fails/times out, `MessagingProviderException` thrown | Provider outage, network partition, high latency, or **provider rate limit exceeded** (fakecomworld: SwiftSend/AsyncFlow 10 req/min, SecurePost 10/min message + 3/min auth, per student group) | `ResourceAccessException`/`HttpStatusCodeException` caught and converted to retry | 4 | 3 | 2 | 24 |
| 6 | Retry pipeline | Retry stages exhausted (10s -> 60s -> 600s) | Message sent to DLQ, notification never delivered | Sustained downstream outage longer than ~11.5 min total backoff | `RabbitMqEventRetryService.determineRoutingKey` defaults to `DLQ_QUEUE` past stage 3 | 5 | 2 | 2 | 20 |
| 7 | RabbitMQ | Queue backlog growth under sustained failure/spike | Increasing notification delivery delay, broker memory pressure | High failure-injection rate combined with high request rate | None automated — manual queue depth monitoring only (Grafana "RabbitMQ Queue Size" panel) | 4 | 2 | 3 | 24 |
| 8 | RabbitMQ | Broker connection/channel exhaustion under high concurrency | Publish failures from `rest-service`, 5xx responses or silently dropped events depending on publisher confirms config | Too many concurrent publishers, no connection pooling limits set | Default Spring AMQP connection factory (no explicit pool tuning) | 4 | 2 | 3 | 24 |
| 9 | PostgreSQL | Connection pool (HikariCP) exhaustion under load | Outbox writes blocked/queued, increased request latency, possible request failures | High concurrent webhook rate vs. default pool size | `hikaricp_connections_active/idle` exposed to Prometheus; "DB Status" Grafana panel | 4 | 3 | 2 | 24 |
| 10 | PostgreSQL | Lock contention / slow outbox writes under sustained load | Increased commit latency, rising `pg_stat_database_xact_rollback` | Long-running transactions, missing index, autovacuum lag | "PostgreSQL Transactions/sec" + "Cache Hit Ratio" panels | 3 | 2 | 2 | 12 |
| 11 | JVM / rest-service | Heap pressure / GC pauses during soak | Latency spikes, possible OOM under prolonged sustained load | Memory leak in outbox/event handling, unbounded retry message accumulation in-process | JVM heap + CPU panels on Grafana dashboard | 4 | 1 | 2 | 8 |
| 12 | Monitoring | Metric scrape gap (RabbitMQ/Postgres exporter down or unreachable) | Blind spot during incident — Grafana panels go flat instead of showing the real failure | Exporter container crash, network issue, plugin not enabled | `InstanceDown` alert only covers `job="openmrsenger"`, not exporters | 3 | 1 | 4 | 12 |

## Fault injection tooling

- **Mock service failure**: `monitoring/stress/webhook-load-test.js` sends a
  deliberately empty `x-provider-config: {}` for `FAILURE_RATE` (default 10%)
  of requests, forcing `MessagingProviderException` and exercising the full
  retry -> DLQ path under load.

## Stress Test Findings

_To be filled in after running `monitoring/stress/webhook-load-test.js`
against the full docker-compose stack. Reference the corresponding row #
above._

| Row # | Observed during test | Triggered? (Y/N) | Notes |
|-------|----------------------|-------------------|-------|
| 2     | Spike load p95/p99 latency | | |
| 5     | Rate-limit (429) responses from fakecomworld | | |
| 6     | DLQ message count after soak | | |
| 7     | Peak `rabbitmq_queue_messages` per queue | | |
| 8     | RabbitMQ connection/channel count under spike | | |
| 9     | Peak `hikaricp_connections_active` vs. pool max | | |
| 11    | JVM heap + CPU trend over soak duration | | |

_Update severity/likelihood/RPN above if the test reveals a failure mode is
more (or less) likely than currently scored._
