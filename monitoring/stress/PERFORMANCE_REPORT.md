# Performance Report — Appointment Webhook Stress Test

> Template. Fill in after running `webhook-load-test.js` (see `README.md`).
> Pull latency/throughput numbers from the k6 summary output
> (`--summary-export=summary.json`) and resource numbers from the Grafana
> "Rest Service - Overview" dashboard / Prometheus queries for the test time
> window.

## Test run metadata

- Date:
- Duration (baseline / spike / soak):
- `BASE_URL`:
- Scenario parameters used (`BASELINE_RPS`, `SPIKE_RPS`, `SOAK_RPS`, `SOAK_DURATION`, `FAILURE_RATE`):
- Stack version / commit:

## Throughput

| Scenario | Target rate (req/s) | Achieved peak throughput (req/s) | Dropped/failed requests |
|----------|----------------------|-----------------------------------|---------------------------|
| Baseline |                      |                                    |                            |
| Spike    |                      |                                    |                            |
| Soak     |                      |                                    |                            |

**Peak throughput overall:** ___ req/s

## Response times

| Scenario | p50 (ms) | p95 (ms) | p99 (ms) | max (ms) |
|----------|----------|----------|----------|----------|
| Baseline |          |          |          |          |
| Spike    |          |          |          |          |
| Soak     |          |          |          |          |

(`http_req_duration` from k6 summary; cross-check against the Grafana "p95
Request Latency" panel for the same window.)

## Resource bottlenecks

| Resource | Metric | Baseline | Spike peak | Soak trend | Bottleneck? |
|----------|--------|----------|------------|------------|-------------|
| CPU (rest-service) | container CPU % | | | | |
| RAM / JVM heap | `jvm_memory_used_bytes{area="heap"}` | | | | |
| DB connections | `hikaricp_connections_active` | | | | |
| DB throughput | `pg_stat_database_xact_commit` rate | | | | |
| RabbitMQ queue depth | `rabbitmq_queue_messages_ready` by queue | | | | |
| RabbitMQ DLQ | `rabbitmq_queue_messages_ready{queue="appointment.events.dlq"}` | | | | |

## Retry mechanism validation

- Failure injection rate used: ___ %
- Messages observed entering `appointment.events.retry.10s`: ___
- Messages observed entering `appointment.events.retry.60s`: ___
- Messages observed entering `appointment.events.retry.600s`: ___
- Messages that reached the DLQ: ___
- Did retries recover successfully when failure injection stopped? (Y/N):

## Findings & recommendations

-
-
-

## Cross-reference

Update `docs/FMEA.md` "Stress Test Findings" table with the row-by-row
results from this run.
