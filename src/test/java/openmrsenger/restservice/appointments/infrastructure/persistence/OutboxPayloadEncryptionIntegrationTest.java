package openmrsenger.restservice.appointments.infrastructure.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import openmrsenger.restservice.appointments.application.ActorDto;
import openmrsenger.restservice.appointments.application.AppointmentService;
import openmrsenger.restservice.appointments.application.FhirAppointmentDto;
import openmrsenger.restservice.appointments.application.ParticipantDto;
import openmrsenger.restservice.appointments.application.TelecomDto;
import openmrsenger.restservice.communications.application.EventRetryService;
import openmrsenger.restservice.communications.application.NotificationEventListener;
import openmrsenger.restservice.communications.application.NotificationLogService;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import openmrsenger.restservice.shared.security.PayloadEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * INTEGRATION TEST: outbox payload encryption.
 *
 * Proves the full GDPR-relevant path end to end against a real (H2) database and the real
 * Spring-configured AES key:
 * 1. processWebhook() -> the row physically stored in outbox_messages.payload is ciphertext;
 *    no patient ID, phone number, or message text is readable in the database.
 * 2. The same ciphertext can be decrypted with the configured PayloadEncryptionService back
 *    into the original NotificationRequestedEvent JSON.
 * 3. NotificationEventListener decrypts that stored ciphertext and dispatches the correct,
 *    plaintext event to the messaging provider.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutboxPayloadEncryptionIntegrationTest {

    private static final String PATIENT_UUID = "11111111-1111-1111-1111-111111111111";
    private static final String PHONE_NUMBER = "+31612345678";
    private static final String APPOINTMENT_UUID = UUID.randomUUID().toString();

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private PayloadEncryptionService encryptionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void scheduleAppointment() {
        FhirAppointmentDto dto = buildDto(APPOINTMENT_UUID, PATIENT_UUID, PHONE_NUMBER, "booked",
                OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));

        appointmentService.processWebhook(dto, "swiftsend", "HOSP-001", "{\"apiKey\":\"secret\"}");
    }

    @Test
    @DisplayName("Stored outbox payload column contains no plaintext patient PII")
    void databaseColumn_ContainsNoPlaintext() {
        List<String> rawPayloads = jdbcTemplate.queryForList("SELECT payload FROM outbox_messages", String.class);

        assertFalse(rawPayloads.isEmpty(), "Expected at least one outbox row to have been written");
        for (String rawPayload : rawPayloads) {
            assertFalse(rawPayload.contains(PATIENT_UUID), "Raw DB payload must not contain the plaintext patient ID");
            assertFalse(rawPayload.contains(PHONE_NUMBER), "Raw DB payload must not contain the plaintext phone number");
            assertFalse(rawPayload.contains("Reminder"), "Raw DB payload must not contain readable message text");
        }
    }

    @Test
    @DisplayName("Stored ciphertext decrypts back to the original event JSON")
    void storedPayload_DecryptsToOriginalEvent() throws Exception {
        OutboxMessageJpaEntity stored = outboxRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new AssertionError("Expected an outbox row"));

        String decryptedJson = encryptionService.decrypt(stored.getPayload());
        NotificationRequestedEvent event = objectMapper.readValue(decryptedJson, NotificationRequestedEvent.class);

        assertEquals(PATIENT_UUID, event.getPatientId());
        assertEquals(PHONE_NUMBER, event.getPhoneNumber());
    }

    @Test
    @DisplayName("NotificationEventListener decrypts the stored ciphertext and dispatches the plaintext event")
    void listener_DecryptsAndDispatches() throws Exception {
        OutboxMessageJpaEntity stored = outboxRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new AssertionError("Expected an outbox row"));

        MessagingProviderPort provider = mock(MessagingProviderPort.class);
        when(provider.supports("SWIFTSEND")).thenReturn(true);
        NotificationLogService logService = mock(NotificationLogService.class);
        EventRetryService retryService = mock(EventRetryService.class);

        NotificationEventListener listener = new NotificationEventListener(
                List.of(provider), objectMapper, logService, retryService, new SimpleMeterRegistry(), encryptionService);

        // The relay forwards the ciphertext verbatim - simulate that here.
        listener.handleNotificationEvent(stored.getPayload(), 0);

        verify(provider).send(argThatMatchesPlaintextEvent(), any());
    }

    private NotificationRequestedEvent argThatMatchesPlaintextEvent() {
        return org.mockito.ArgumentMatchers.argThat(event ->
                event != null
                        && PATIENT_UUID.equals(event.getPatientId())
                        && PHONE_NUMBER.equals(event.getPhoneNumber()));
    }

    private FhirAppointmentDto buildDto(String appointmentUuid, String patientUuid, String phone, String status, OffsetDateTime start) {
        FhirAppointmentDto dto = new FhirAppointmentDto();
        dto.setResourceType("Appointment");
        dto.setId(appointmentUuid);
        dto.setStatus(status);
        dto.setStart(start.toString());

        ParticipantDto participant = new ParticipantDto();
        ActorDto actor = new ActorDto();
        actor.setReference("Patient/" + patientUuid);
        actor.setDisplay("John Doe");

        TelecomDto telecom = new TelecomDto();
        telecom.setSystem("phone");
        telecom.setValue(phone);

        actor.setTelecom(List.of(telecom));
        participant.setActor(actor);

        dto.setParticipant(List.of(participant));
        return dto;
    }
}
