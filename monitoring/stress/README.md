# Webhook Stress Test

Load test for `POST /api/webhooks/appointments`, the entry point that turns an
OpenMRS appointment event into a queued outbound notification (SMS/email via
SwiftSend, AsyncFlow, SecurePost or LegacyLink).

## Prerequisites

- Full stack running: `docker compose up -d` from `rest-service/`.
- [k6](https://k6.io/docs/get-started/installation/) installed locally.
- Grafana at http://localhost:3000 (admin/admin), Prometheus at http://localhost:9090.

## Running

Export the same provider credentials used in `rest-service/.env` so injected
requests authenticate against the real fakecomworld test double:

```bash
export $(grep -v '^#' ../../.env | xargs)
k6 run webhook-load-test.js
```

Override scenario sizing without editing the script:

```bash
k6 run \
  -e BASE_URL=http://localhost:8888 \
  -e BASELINE_RPS=20 -e SPIKE_RPS=300 -e SOAK_RPS=30 -e SOAK_DURATION=30m \
  -e FAILURE_RATE=0.1 \
  --out json=results.json \
  webhook-load-test.js
```

`--summary-export=summary.json` (or `--out json=...` for full raw samples)
is what feeds the numbers into `PERFORMANCE_REPORT.md`.

## Scenarios

| Scenario      | Executor               | Default load                          | Purpose |
|---------------|-------------------------|----------------------------------------|---------|
| `baseline_load` | constant-arrival-rate | 20 req/s for 2m                        | Steady-state latency/throughput baseline |
| `spike_load`    | ramping-arrival-rate  | 20 -> 300 -> 20 req/s over 2m          | Burst tolerance, queue backlog behavior |
| `soak_test`     | constant-arrival-rate | 30 req/s for 10m (raise via `SOAK_DURATION` for a real soak, e.g. 1-2h) | Memory/connection leaks, sustained DB & RabbitMQ pressure |

All three run back-to-back in one `k6 run` invocation (see `startTime` in
`options.scenarios`), so one summary covers the full test.

## Mixed providers & failure injection

Each request picks a random provider (`SWIFTSEND`, `ASYNCFLOW`, `SECUREPOST`,
`LEGACYLINK`) via the `x-messaging-provider` header, matching the production
header contract on `AppointmentWebhookController`.

`FAILURE_RATE` (default `0.1`) controls the fraction of requests sent with a
deliberately empty `x-provider-config: {}` header. `AbstractRestMessagingAdapter`
rejects this with a `MessagingProviderException`, which routes the event
through the retry pipeline (`appointment.events.retry.10s` -> `.60s` -> `.600s`
-> `appointment.events.dlq`). This is the "mock service failure" injection
required to validate the retry mechanism under load — watch the **RabbitMQ
Queue Size** panel in Grafana for retry queue depth and DLQ growth during and
after the test.

## Per-provider stats

The script tracks duration/success/failure per provider and prints a
**PER-PROVIDER STATS** table at the end of the run (total, success, failed,
err%, avg/p95/p99 latency), in addition to the normal k6 summary. The same
data is written to `provider-stats.json` for the performance report. If one
provider's err% is far higher than the others, check that provider's
credentials/env var first — webhook responses are always `200` regardless of
downstream provider failures (the provider call happens async), so HTTP-level
failures are not where provider auth problems show up; the per-provider
table is.

## Report output

Every run of `handleSummary()` (`webhook-load-test.js`) writes, into the
current directory:

- `provider-stats.json` — per-provider table as JSON.
- `summary.html` — full interactive HTML report ([k6-reporter](https://github.com/benc-uk/k6-reporter)): all scenario metrics, thresholds, checks.

For the raw k6 summary itself as JSON/text, use the standard k6 flags:

```bash
k6 run --summary-export=summary.json webhook-load-test.js   # summary stats
k6 run --out json=results.json webhook-load-test.js         # full raw samples
```

## What to watch in Grafana while it runs

Dashboard: **Rest Service - Overview** (http://localhost:3000/d/rest-service)

- HTTP Request Rate / p95 Request Latency — throughput and latency under load.
- RabbitMQ Queue Size — backlog and DLQ growth from injected failures.
- PostgreSQL Active Connections / Cache Hit Ratio — DB pressure.
- JVM Heap Memory — leak/GC pressure during the soak phase.

After a run, fill in `PERFORMANCE_REPORT.md` and the "Stress Test Findings"
section of `../../docs/FMEA.md` with the observed numbers.
