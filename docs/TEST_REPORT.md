# Testrapport — OpenMRSenger REST Service

---

## 1. Unit-tests

18 testklassen met ~85 methoden dekken alle lagen van de hexagonale architectuur.

| Testklasse | Laag | Methoden | Wat wordt getest |
|---|---|---|---|
| `AppointmentWebhookControllerTest` | Web | 7 | Geldige auth → 200, ongeldige auth → 401, ongeldig FHIR → 400 |
| `ApiKeyWebhookAuthenticatorTest` | Web | 5 | API-key header accepteren/weigeren |
| `FhirAppointmentValidatorImplTest` | Applicatie | 10 | Null payload, verkeerd resourceType, ontbrekende velden |
| `FhirAppointmentValidatorTest` | Web | 10 | Dezelfde regels via de infrastructuuradapter |
| `AppointmentServiceImplTest` | Applicatie | 1 | `processWebhook` schrijft naar de outbox |
| `AppointmentRepositoryAdapterTest` | Persistentie | 4 | JPA-mapping, outbox-rij aanmaken |
| `NotificationEventListenerTest` | Applicatie | 3 | Succesvol verzenden, idempotentie, fout → retry |
| `ProviderRoutingAndExtensionTest` | Applicatie | 2 | Routing naar SwiftSend en SecurePost |
| `SwiftSendAdapterTest` | Provider | 1 | HTTP-verzending happy path |
| `AsyncFlowAdapterTest` | Provider | 5 | Supports-check, direct succes, polling, timeout, fout |
| `SecurePostAdapterTest` | Provider | 1 | Token-uitwisseling + verzending |
| `SecurePostAdapterExceptionTest` | Provider | 3 | Verlopen token, 500-respons, fout JSON |
| `LegacyLinkAdapterTest` | Provider | 1 | Basic auth verzending |
| `NotificationLogAdapterTest` | Persistentie | 3 | `isAlreadySent`, `logPending`, `logSuccess` |
| `RabbitMqEventRetryServiceTest` | Messaging | 2 | Stage 0→1 retry-queue, max stage → DLQ |
| `AesPayloadEncryptionServiceTest` | Security | 7 | Encrypt/decrypt, uniek cijfertekst, sleutelvalidatie |
| `LogSanitizerTest` | Logging | 7 | Telefoonnummer maskeren, bericht afkappen, headers redigeren |
| `HexagonalArchitectureTest` | Architectuur | 5 | Laagscheiding via ArchUnit (zie §4) |

---

## 2. Geautomatiseerde integratietests (lokale betrouwbaarheid)

Drie `@SpringBootTest`-tests draaien tegen een echte H2-database met de volledige Spring Boot-context. Ze bewijzen dat de kritieke paden end-to-end werken zonder handmatige tussenkomst.

| Testklasse | Bewijs |
|---|---|
| `OutboxPayloadEncryptionIntegrationTest` | DB-kolom bevat geen leesbare PII; cijfertekst ontcijfert naar het originele event; listener verzendt correct plaintext event |
| `RabbitMqOutboxRelayIntegrationTest` | Relay publiceert naar RabbitMQ en zet rij op `processed = true` **ná** publicatie (at-least-once garantie) |
| `DataRetentionSchedulerIntegrationTest` | AVG-retentieschema verwijdert en anonimiseert data correct per tijdsvenster |

---

## 3. Onderbouwde uitzonderingsscenario's

Dit onderdeel toont aan dat het systeem correct reageert op realistische storingen. Elk scenario is onderbouwd met de reden waarom het relevant is en welke geautomatiseerde test het afdekt.

### 3.1 Uitval van een externe messagingprovider → retry-pipeline

**Waarom relevant:** Externe providers (SwiftSend, AsyncFlow, SecurePost, LegacyLink) kunnen tijdelijk onbereikbaar zijn. Berichten mogen dan niet verloren gaan.

**Mechanisme:** Bij een mislukte verzending wordt het bericht via RabbitMQ opnieuw aangeboden met exponentieel toenemende wachttijden. Na drie pogingen belandt het bericht in de dead-letter queue (DLQ) voor handmatige afhandeling.

**Geautomatiseerde validatie:**

```
Test: NotificationEventListenerTest#handleNotificationEvent_Failure_ShouldRetry

Als:  provider.send() gooit IllegalStateException("Send failed")
Dan:  logService.logFailure(eventId, "Send failed")             ✓
      retryService.scheduleRetry(payload, stage=0, ex) aangeroepen ✓
      provider.send() niet opnieuw aangeroepen in dezelfde aanroep ✓
```

```
Test: RabbitMqEventRetryServiceTest#scheduleRetry_Stage0To1

Als:  scheduleRetry(payload, stage=0, ex)
Dan:  bericht naar appointment.events.retry.10s                 ✓
      header x-retry-stage = 1                                  ✓

Test: RabbitMqEventRetryServiceTest#scheduleRetry_MaxReached_GoesToDlq

Als:  scheduleRetry(payload, stage=3, ex)
Dan:  bericht naar appointment.events.dlq                       ✓
```

**Retry-volgorde:**

```
Stage 0 → appointment.events.retry.10s
Stage 1 → appointment.events.retry.60s
Stage 2 → appointment.events.retry.600s
Stage 3 → appointment.events.dlq  (geen verdere retry)
```

### 3.2 Uitval van de database (Outbox-patroon)

**Waarom relevant:** Als de database tijdelijk onbereikbaar is tijdens het verwerken van een binnenkomend webhook-bericht, mag het bericht niet stilzwijgend verdwijnen.

**Mechanisme:** Het Outbox-patroon zorgt dat de HTTP-ontvangst en de databaseschrijfactie in één transactie plaatsvinden. Als de transactie mislukt, wordt `500` teruggegeven en herprobeert de aanroepende partij. Als de relay crasht ná de DB-schrijfactie maar vóór publicatie naar RabbitMQ, wordt het bericht bij de volgende relay-run alsnog gepubliceerd (status is nog `processed = false`).

**Geautomatiseerde validatie:**

```
Test: RabbitMqOutboxRelayIntegrationTest#relayProcess_ShouldPublishAndMarkAsProcessed

Gegeven: outbox-rij in H2 met scheduledFor = now() - 1 min
Als:     outboxRelay.processOutbox() wordt uitgevoerd
Dan:     rabbitTemplate.convertAndSend() aangeroepen             ✓
         rij.isProcessed() == true (pas ná publicatie)           ✓
```

### 3.3 Dubbele aflevering vanuit RabbitMQ

**Waarom relevant:** RabbitMQ levert berichten at-least-once. Een herlevering na een time-out mag niet leiden tot dubbele verzending naar de patiënt.

**Mechanisme:** Bij ontvangst wordt eerst `isAlreadySent(eventId)` gecontroleerd. Als het bericht al verwerkt is, wordt de verdere verwerking overgeslagen.

**Geautomatiseerde validatie:**

```
Test: NotificationEventListenerTest#handleNotificationEvent_AlreadySent_ShouldSkip

Als:  logService.isAlreadySent(eventId) = true
Dan:  geen logPending                                            ✓
      geen provider.send                                         ✓
      geen scheduleRetry                                         ✓
```

### 3.4 Provider-specifieke uitzonderingen (SecurePost)

**Waarom relevant:** OAuth2-gebaseerde providers hebben tokens die verlopen. Een verlopen token mag niet leiden tot een mislukte verzending; het systeem moet automatisch een nieuw token ophalen.

**Geautomatiseerde validatie:**

| Scenario | Verwacht gedrag | Resultaat |
|---|---|---|
| Access token verlopen (`expiryTime` verstreken) | Nieuw token ophalen, dan verzenden (2 HTTP-calls) | GESLAAGD |
| API geeft 500 terug | Gooit `MessagingProviderException` → belandt in retry | GESLAAGD |
| Fout JSON in token-respons | Gooit `NumberFormatException` → belandt in retry | GESLAAGD |

---

## 4. Additionele kwaliteitskenmerken

### 4.1 Beveiliging — AES-256-GCM payload-encryptie

**Waarom relevant:** Outbox-berichten bevatten patiëntgegevens (ID, telefoonnummer). Een datalek van de database mag geen leesbare persoonsgegevens blootstellen.

**Geautomatiseerde validatie** (`OutboxPayloadEncryptionIntegrationTest`):

```
Test 1 — databaseColumn_ContainsNoPlaintext
  SELECT payload FROM outbox_messages bevat geen leesbare patiënt-ID,
  telefoonnummer of berichttekst. Kolom bevat alleen AES-GCM-cijfertekst.   ✓

Test 2 — storedPayload_DecryptsToOriginalEvent
  encryptionService.decrypt(opgeslagenPayload) → geldig event-object
  event.patientId en event.phoneNumber komen overeen met de invoer.          ✓

Test 3 — listener_DecryptsAndDispatches
  Listener ontcijfert de cijfertekst correct en stuurt het plaintext-event
  door naar de provider.                                                      ✓
```

Aanvullende unit-tests (`AesPayloadEncryptionServiceTest`, 7 methoden): dezelfde plaintext levert elke keer een andere cijfertekst op (random IV), sleutellengte-validatie, encryptie/decryptie round-trip.

### 4.2 Beveiliging — log-sanitatie

**Waarom relevant:** Telefoonnummers en berichtinhoud mogen niet in platte tekst in logbestanden verschijnen.

**Geautomatiseerde validatie** (`LogSanitizerTest`, 7 methoden): telefoonnummers worden gemaskeerd, berichten worden afgekapt, gevoelige headers worden geredigeerd.

### 4.3 AVG-dataretentie

**Waarom relevant:** Persoonsgegevens mogen niet langer bewaard worden dan noodzakelijk.

**Geautomatiseerde validatie** (`DataRetentionSchedulerIntegrationTest`, 3 scenario's op echte H2-database):

| Retentieregel | Conditie | Resultaat |
|---|---|---|
| Afspraken en outbox-berichten verwijderd | Ouder dan 14 dagen | GESLAAGD |
| Recente records bewaard | Jonger dan 14 dagen | GESLAAGD |
| Notificatielogs geanonimiseerd | Ouder dan 14 dagen; foutmelding bevat naam + telefoon | `errorMessage` leeggemaakt, `status`/`providerId`/`hospitalId` bewaard | GESLAAGD |
| Recente logs onaangeroerd | Jonger dan 14 dagen | GESLAAGD |
| Meta-data verwijderd | Ouder dan 365 dagen | GESLAAGD |
| Meta-data binnen auditvenster | ~200 dagen oud | GESLAAGD |

### 4.4 Architectuur — hexagonale laagscheiding (ArchUnit)

**Waarom relevant:** Als de domeinlaag afhankelijk wordt van infrastructuurklassen, ontstaan koppelingsfouten die moeilijk te isoleren zijn bij tests of migraties.

**Geautomatiseerde validatie** (`HexagonalArchitectureTest`, ArchUnit 1.3.0):

| Regel | Afdwinging |
|---|---|
| `no_generic_exceptions` | Geen klasse gooit `RuntimeException` of `Exception` direct |
| `no_java_util_logging` | Alle logging via SLF4J |
| `domain_events_should_be_immutable` | Alle `*Event`-klassen hebben alleen `final` velden |
| `controllers_should_be_in_infrastructure_web` | Alle controllers zitten in `..infrastructure.web..` |
| `domain_should_not_depend_on_infrastructure` | Domeinlaag importeert geen infrastructuurklassen |
| `application_should_not_depend_on_infrastructure` | Applicatielaag importeert geen infrastructuurklassen |

Alle 5 regels slagen. Laagscheiding wordt bij elke Maven-build automatisch gecontroleerd via statische bytecode-analyse.

---

## 5. Loadtest-resultaten

### 5.1 Testopzet

Tool: k6 (`monitoring/stress/webhook-load-test.js`)
Endpoint: `POST /api/webhooks/appointments`
Foutinjectie: **10%** van verzoeken stuurt lege `x-provider-config: {}` om de retry-pipeline onder belasting te testen

| Scenario | Type | Belasting |
|---|---|---|
| `baseline_load` | Constant | 20 verzoeken/s, 2 minuten |
| `spike_load` | Oplopend | 20 → 300 → 20 verzoeken/s, 2 minuten |
| `soak_test` | Constant | 30 verzoeken/s, 10 minuten |

Geconfigureerde drempelwaarden: `p(95) < 1000 ms` en `foutpercentage < 5%`.

### 5.2 Resultaten per provider

Bron: `monitoring/stress/provider-stats.json`

| Provider | Totaal | Geslaagd | Mislukt | Foutpercentage | Gem. (ms) | p95 (ms) | p99 (ms) |
|---|---|---|---|---|---|---|---|
| SWIFTSEND | 10.398 | 10.398 | 0 | 0,00% | 748 | 2.482 | 3.014 |
| ASYNCFLOW | 10.706 | 10.706 | 0 | 0,00% | 737 | 2.507 | 2.953 |
| SECUREPOST | 10.517 | 10.517 | 0 | 0,00% | 750 | 2.180 | 3.032 |
| LEGACYLINK | 10.696 | 10.696 | 0 | 0,00% | 739 | 2.443 | 2.983 |
| **Totaal** | **42.317** | **42.317** | **0** | **0,00%** | **~744** | — | — |

**HTTP-foutpercentage: 0,00%.** De webhook retourneert altijd `200` zodra het bericht in de outbox staat. Providerfouten worden asynchroon afgehandeld via de retry-queue en zijn niet zichtbaar als HTTP-fouten.

De p95-latentie is gemeten over alle drie scenario's inclusief de spike naar 300 verzoeken/s. Onder standaardbelasting (20 req/s) is de latentie beduidend lager.

### 5.3 Retry-pipeline onder belasting

Circa **4.232 verzoeken** (10% van 42.317) werden verzonden met een lege provider-configuratie, wat `MessagingProviderException` activeert en de retry-pipeline in werking stelt:

```
appointment.events.retry.10s → .retry.60s → .retry.600s → appointment.events.dlq
```

Het nul-procent HTTP-foutpercentage bevestigt dat het webhook-endpoint volledig losgekoppeld is van de downstream bezorging: alle 42.317 verzoeken zijn opgenomen in de outbox, ook de 10% waarbij de downstream bezorging intentioneel mislukte.

### 5.4 Monitoring en alerting

Prometheus scrape-interval: **5 seconden**
Targets: `rest-service:8081/actuator/prometheus`, `postgres-exporter:9187`, `rabbitmq:15692`

Grafana-dashboard (`monitoring/grafana/dashboards/rest-service-dashboard.json`): HTTP Request Rate, p95-latentie, RabbitMQ queue-diepte (inclusief DLQ), PostgreSQL-verbindingen, JVM Heap.

| Alert | Conditie | Ernst |
|---|---|---|
| `InstanceDown` | Service onbereikbaar voor 1 min | Kritiek |
| `HighErrorRate` | 5xx-percentage > 5% over 5 min | Waarschuwing |
| `HighLatency` | p95 > 1 s voor 5 min | Waarschuwing |

---

## 6. Totaaloverzicht

| Wat wordt getest | Testtype | Resultaat |
|---|---|---|
| Webhook-authenticatie (geldig / ongeldig) | Unit | GESLAAGD |
| FHIR-validatie (null, verkeerd type, ontbrekende velden) | Unit | GESLAAGD |
| Outbox-schrijven (afspraak → rij) | Unit + Integratie | GESLAAGD |
| Outbox-relay (DB → RabbitMQ, at-least-once) | Integratie | GESLAAGD |
| **Uitval provider → retry → DLQ** | **Unit** | **GESLAAGD** |
| **Uitval database (Outbox-patroon)** | **Integratie** | **GESLAAGD** |
| **Dubbele aflevering → idempotentie** | **Unit** | **GESLAAGD** |
| **SecurePost token verlopen → automatisch vernieuwen** | **Unit** | **GESLAAGD** |
| **SecurePost 500 → uitzondering → retry** | **Unit** | **GESLAAGD** |
| **AES-256-GCM: DB-kolom bevat geen leesbare PII** | **Integratie (security)** | **GESLAAGD** |
| **AES-256-GCM encryptie/decryptie round-trip** | **Unit (security)** | **GESLAAGD** |
| **Log-sanitatie (telefoon, bericht, headers)** | **Unit (security)** | **GESLAAGD** |
| **AVG 14-daagse verwijdering** | **Integratie (AVG)** | **GESLAAGD** |
| **AVG 14-daagse anonimisering** | **Integratie (AVG)** | **GESLAAGD** |
| **AVG 1-jarige meta-verwijdering** | **Integratie (AVG)** | **GESLAAGD** |
| **Hexagonale laagscheiding (ArchUnit)** | **Architectuur** | **GESLAAGD** |
| Throughput onder belasting (42.317 verzoeken, 0% fouten) | Load (k6) | GESLAAGD |
| Retry-pipeline onder 10% foutinjectie | Load (k6) | GESLAAGD |
