# OpenMRSenger - rest-service

A Spring Boot service that receives appointment webhooks from OpenMRS and sends patient reminder notifications via configurable messaging providers.

## What it does

When an appointment is created or updated in OpenMRS, a webhook is sent to this service. It then schedules reminder notifications for the patient at 24 hours and 1 hour before the appointment. If an appointment is cancelled after a reminder was already sent, the patient also receives an immediate cancellation notification.

Notifications are dispatched through one of four messaging providers:

- **SwiftSend** — API key authentication
- **AsyncFlow** — API key authentication
- **SecurePost** — OAuth2 client credentials
- **LegacyLink** — Basic authentication

Messages are queued through RabbitMQ using a transactional outbox pattern to guarantee delivery.

## Tech stack

- Java 21, Spring Boot 4
- PostgreSQL (persistence + outbox)
- RabbitMQ (message broker)
- Prometheus + Grafana (monitoring)

## Starting up

### Prerequisites

- Docker and Docker Compose
- A `.env` file in the project root (see below)

### Environment variables

Create a `.env` file:

```env
BASE_API_URL=https://fakecomworld.endpoint
STUDENT_GROUP=3

POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_DB=restservice_db

RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest

STUDENT_GROUP=Group1

SWIFT_SEND_API_KEY=
ASYNC_FLOW_API_KEY=

SECURE_POST_CLIENT_ID=
SECURE_POST_CLIENT_SECRET=

LEGACY_LINK_USERNAME=
LEGACY_LINK_PASSWORD=
```

### Run with Docker Compose

```bash
docker-compose up --build
```

This starts all services: the rest-service, PostgreSQL, RabbitMQ, FakeComWorld (mock messaging API), Prometheus, Grafana, and Alertmanager.

| Service       | URL                        |
|---------------|----------------------------|
| rest-service  | http://localhost:8888       |
| RabbitMQ UI   | http://localhost:15672      |
| Prometheus    | http://localhost:9090       |
| Grafana       | http://localhost:3000       |
| Alertmanager  | http://localhost:9093       |

Grafana default credentials: `admin` / `admin`

### Run locally (without Docker)

Make sure PostgreSQL and RabbitMQ are running locally, then:

```bash
./mvnw spring-boot:run
```

The service starts on port `8081`.

## Webhook endpoint

```
POST /api/webhooks/appointments
```

Required headers:

| Header                | Description                                      |
|-----------------------|--------------------------------------------------|
| `Authorization`       | Bearer token matching `webhook.secret`           |
| `x-messaging-provider` | One of: `SWIFT_SEND`, `ASYNC_FLOW`, `SECURE_POST`, `LEGACY_LINK` |
| `x-hospital-name`     | Name of the hospital                             |
| `x-provider-config`   | JSON string with provider credentials (optional) |
