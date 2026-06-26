import http from 'k6/http';
import { check } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';

// ---------------------------------------------------------------------------
// OpenMRSenger - Appointment Webhook Stress Test
//
// Target: POST {BASE_URL}/api/webhooks/appointments
//
// Run examples:
//   k6 run monitoring/stress/webhook-load-test.js
//   k6 run -e BASE_URL=http://localhost:8888 -e SOAK_DURATION=30m monitoring/stress/webhook-load-test.js
//   k6 run --out json=monitoring/stress/results.json monitoring/stress/webhook-load-test.js
//
// Required env vars (same values as rest-service/.env so requests authenticate
// against the real provider credentials configured for fakecomworld):
//   WEBHOOK_SECRET            (default: my-secret-key, matches webhook.secret default)
//   SWIFT_SEND_API_KEY
//   ASYNC_FLOW_API_KEY
//   SECURE_POST_CLIENT_ID
//   SECURE_POST_CLIENT_SECRET
//   LEGACY_LINK_USERNAME
//   LEGACY_LINK_PASSWORD
//
// Three scenarios run back-to-back in a single invocation: baseline_load,
// spike_load, soak_test. Durations/rates are tunable via env vars so the
// same script can be used for a quick smoke run or a long soak run.
// ---------------------------------------------------------------------------

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8888';
const WEBHOOK_SECRET = __ENV.WEBHOOK_SECRET || 'my-secret-key';
const HOSPITAL_NAME = __ENV.HOSPITAL_NAME || 'Stress-Test-Hospital';

// Fraction of requests that intentionally carry a broken provider config,
// simulating a mock downstream-service failure so the retry/DLQ pipeline
// (appointment.events.retry.10s/60s/600s -> appointment.events.dlq) gets
// exercised under load.
const FAILURE_INJECTION_RATE = Number(__ENV.FAILURE_RATE || 0.1);

const failureInjected = new Counter('failure_injected_requests');
const webhookDuration = new Trend('webhook_request_duration', true);

const PROVIDERS = [
  {
    id: 'SWIFTSEND',
    validConfig: () => JSON.stringify({ apiKey: __ENV.SWIFT_SEND_API_KEY || 'invalid' }),
  },
  {
    id: 'ASYNCFLOW',
    validConfig: () => JSON.stringify({ apiKey: __ENV.ASYNC_FLOW_API_KEY || 'invalid' }),
  },
  {
    id: 'SECUREPOST',
    validConfig: () =>
      JSON.stringify({
        clientId: __ENV.SECURE_POST_CLIENT_ID || 'invalid',
        clientSecret: __ENV.SECURE_POST_CLIENT_SECRET || 'invalid',
      }),
  },
  {
    id: 'LEGACYLINK',
    validConfig: () =>
      JSON.stringify({
        username: __ENV.LEGACY_LINK_USERNAME || 'invalid',
        password: __ENV.LEGACY_LINK_PASSWORD || 'invalid',
      }),
  },
];

function randomProvider() {
  return PROVIDERS[Math.floor(Math.random() * PROVIDERS.length)];
}

// Per-provider duration/success/failure metrics, surfaced in handleSummary()
// below as a "PER-PROVIDER STATS" table appended to the default k6 summary.
const providerMetrics = {};
PROVIDERS.forEach((p) => {
  const key = p.id.toLowerCase();
  providerMetrics[p.id] = {
    duration: new Trend(`webhook_duration_${key}`, true),
    success: new Counter(`webhook_success_${key}`),
    failed: new Counter(`webhook_failed_${key}`),
  };
});

// FHIR Appointment resource shape, matching FhirAppointmentValidator:
// resourceType="Appointment", valid status, parseable start, non-empty
// participant[] with actor.reference (Patient/Practitioner) + telecom.
function buildPayload(tag) {
  const now = new Date();
  const start = new Date(now.getTime() + 24 * 60 * 60 * 1000);
  const end = new Date(start.getTime() + 30 * 60 * 1000);
  const id = `${tag}-${__VU}-${__ITER}-${Math.floor(Math.random() * 1e6)}`;

  return JSON.stringify({
    resourceType: 'Appointment',
    id: `appt-${id}`,
    status: 'booked',
    start: start.toISOString(),
    end: end.toISOString(),
    participant: [
      {
        status: 'accepted',
        actor: {
          reference: `Patient/patient-${id}`,
          display: `Stress Patient ${id}`,
          telecom: [{ system: 'phone', value: '+31600000000' }],
        },
      },
      {
        status: 'accepted',
        actor: {
          reference: `Practitioner/dr-${id}`,
          display: 'Dr. Load Test',
        },
      },
    ],
  });
}

export function webhookRequest() {
  const provider = randomProvider();
  const injectFailure = Math.random() < FAILURE_INJECTION_RATE;

  const headers = {
    'Content-Type': 'application/json',
    Authorization: WEBHOOK_SECRET,
    'x-messaging-provider': provider.id,
    'x-hospital-name': HOSPITAL_NAME,
  };

  if (injectFailure) {
    failureInjected.add(1);
    // Malformed/empty config -> AbstractRestMessagingAdapter throws
    // MessagingProviderException -> message routed to retry queues -> DLQ.
    headers['x-provider-config'] = '{}';
  } else {
    headers['x-provider-config'] = provider.validConfig();
  }

  const res = http.post(`${BASE_URL}/api/webhooks/appointments`, buildPayload(provider.id), {
    headers,
    tags: { provider: provider.id, injected_failure: String(injectFailure) },
  });

  webhookDuration.add(res.timings.duration);

  const metrics = providerMetrics[provider.id];
  metrics.duration.add(res.timings.duration);
  if (res.status === 200) {
    metrics.success.add(1);
  } else {
    metrics.failed.add(1);
  }

  check(res, {
    'status is 200 (accepted into outbox)': (r) => r.status === 200,
  });
}

export function handleSummary(data) {
  const rows = PROVIDERS.map((p) => {
    const key = p.id.toLowerCase();
    const d = (data.metrics[`webhook_duration_${key}`] || {}).values || {};
    const success = ((data.metrics[`webhook_success_${key}`] || {}).values || {}).count || 0;
    const failed = ((data.metrics[`webhook_failed_${key}`] || {}).values || {}).count || 0;
    const total = success + failed;
    const errRate = total ? ((failed / total) * 100).toFixed(2) : '0.00';
    return {
      provider: p.id,
      total,
      success,
      failed,
      errRate,
      avg: (d.avg || 0).toFixed(2),
      p95: (d['p(95)'] || 0).toFixed(2),
      p99: (d['p(99)'] || 0).toFixed(2),
    };
  });

  const col = (s, w) => String(s).padEnd(w);
  let table = '\n  █ PER-PROVIDER STATS\n\n';
  table += `    ${col('provider', 12)}${col('total', 8)}${col('success', 9)}${col('failed', 8)}${col('err%', 8)}${col('avg(ms)', 9)}${col('p95(ms)', 9)}p99(ms)\n`;
  rows.forEach((r) => {
    table += `    ${col(r.provider, 12)}${col(r.total, 8)}${col(r.success, 9)}${col(r.failed, 8)}${col(r.errRate, 8)}${col(r.avg, 9)}${col(r.p95, 9)}${r.p99}\n`;
  });

  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }) + table,
    'provider-stats.json': JSON.stringify(rows, null, 2),
    'summary.html': htmlReport(data, { title: 'Appointment Webhook Stress Test' }),
  };
}

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    baseline_load: {
      executor: 'constant-arrival-rate',
      exec: 'webhookRequest',
      rate: Number(__ENV.BASELINE_RPS || 20),
      timeUnit: '1s',
      duration: __ENV.BASELINE_DURATION || '2m',
      preAllocatedVUs: 20,
      maxVUs: 50,
      startTime: '0s',
      tags: { test_phase: 'baseline' },
    },
    spike_load: {
      executor: 'ramping-arrival-rate',
      exec: 'webhookRequest',
      timeUnit: '1s',
      startTime: __ENV.SPIKE_START || '2m30s',
      preAllocatedVUs: 50,
      maxVUs: 400,
      stages: [
        { target: Number(__ENV.SPIKE_RPS || 300), duration: '30s' },
        { target: Number(__ENV.SPIKE_RPS || 300), duration: __ENV.SPIKE_HOLD || '1m' },
        { target: Number(__ENV.BASELINE_RPS || 20), duration: '30s' },
      ],
      tags: { test_phase: 'spike' },
    },
    soak_test: {
      executor: 'constant-arrival-rate',
      exec: 'webhookRequest',
      rate: Number(__ENV.SOAK_RPS || 30),
      timeUnit: '1s',
      duration: __ENV.SOAK_DURATION || '10m',
      preAllocatedVUs: 30,
      maxVUs: 100,
      startTime: __ENV.SOAK_START || '5m',
      tags: { test_phase: 'soak' },
    },
  },
  thresholds: {
    // Baseline/soak: outbox accept is fast; spike allows degradation under 300 RPS burst.
    // Error rate is tight because injected failures still return 200 (accepted into outbox,
    // failed asynchronously via retry queues -> DLQ) — only real infra errors count here.
    'http_req_duration{test_phase:baseline}': ['p(95)<500'],
    'http_req_duration{test_phase:spike}': ['p(95)<3000'],
    'http_req_duration{test_phase:soak}': ['p(95)<500'],
    'http_req_failed{test_phase:baseline}': ['rate<0.02'],
    'http_req_failed{test_phase:spike}': ['rate<0.05'],
    'http_req_failed{test_phase:soak}': ['rate<0.02'],
  },
};
