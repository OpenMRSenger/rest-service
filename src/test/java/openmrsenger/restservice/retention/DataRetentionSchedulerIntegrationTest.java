package openmrsenger.restservice.retention;

import openmrsenger.restservice.appointments.infrastructure.persistence.AppointmentJpaEntity;
import openmrsenger.restservice.appointments.infrastructure.persistence.OutboxMessageJpaEntity;
import openmrsenger.restservice.appointments.infrastructure.persistence.SpringDataAppointmentRepository;
import openmrsenger.restservice.appointments.infrastructure.persistence.SpringDataOutboxRepository;
import openmrsenger.restservice.communications.infrastructure.persistence.NotificationLogJpaEntity;
import openmrsenger.restservice.communications.infrastructure.persistence.SpringDataNotificationLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * INTEGRATION TEST: DataRetentionScheduler
 *
 * Proves the GDPR retention rules end to end against a real (H2) database:
 * 1. Appointments and outbox messages (which carry patient-identifiable payloads)
 *    older than 14 days are permanently deleted, recent ones are kept.
 * 2. Notification logs older than 14 days are anonymized: free-text error detail is
 *    stripped, but status/timestamps/provider meta-information needed for auditing
 *    and invoice verification is retained.
 * 3. Notification log meta-information is purged entirely once it exceeds the
 *    one-year audit retention window.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataRetentionSchedulerIntegrationTest {

    @Autowired
    private DataRetentionScheduler scheduler;

    @Autowired
    private SpringDataAppointmentRepository appointmentRepository;

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private SpringDataNotificationLogRepository notificationLogRepository;

    @Test
    @DisplayName("Purges appointments and outbox messages older than 14 days, keeps recent ones")
    void purgeIdentifiableData_RemovesOnlyExpiredRecords() {
        AppointmentJpaEntity oldAppointment = new AppointmentJpaEntity(
                UUID.randomUUID(), "PAT-OLD", LocalDateTime.now(ZoneOffset.UTC).minusDays(20), "Completed");
        AppointmentJpaEntity recentAppointment = new AppointmentJpaEntity(
                UUID.randomUUID(), "PAT-NEW", LocalDateTime.now(ZoneOffset.UTC).minusDays(1), "Scheduled");
        appointmentRepository.save(oldAppointment);
        appointmentRepository.save(recentAppointment);

        OutboxMessageJpaEntity oldMessage = new OutboxMessageJpaEntity(
                "appointment.events", "{\"patientReference\":\"PAT-OLD\"}",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(20), null, UUID.randomUUID());
        oldMessage.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(20));
        OutboxMessageJpaEntity recentMessage = new OutboxMessageJpaEntity(
                "appointment.events", "{\"patientReference\":\"PAT-NEW\"}",
                OffsetDateTime.now(ZoneOffset.UTC).minusDays(1), null, UUID.randomUUID());
        recentMessage.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(1));
        UUID oldMessageId = outboxRepository.save(oldMessage).getId();
        UUID recentMessageId = outboxRepository.save(recentMessage).getId();

        scheduler.purgeIdentifiableData();

        assertTrue(appointmentRepository.findById(oldAppointment.getId()).isEmpty(), "Old appointment must be deleted");
        assertTrue(appointmentRepository.findById(recentAppointment.getId()).isPresent(), "Recent appointment must be kept");
        assertTrue(outboxRepository.findById(oldMessageId).isEmpty(), "Old outbox message must be deleted");
        assertTrue(outboxRepository.findById(recentMessageId).isPresent(), "Recent outbox message must be kept");
    }

    @Test
    @DisplayName("Anonymizes notification logs older than 14 days, keeps meta-information for auditing")
    void anonymizeAgedNotificationLogs_StripsErrorDetailButKeepsMetaInformation() {
        UUID oldEventId = UUID.randomUUID();
        NotificationLogJpaEntity oldLog = new NotificationLogJpaEntity(oldEventId);
        oldLog.setStatus("FAILED");
        oldLog.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(20));
        oldLog.setProcessedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(20));
        oldLog.setErrorMessage("Delivery failed for patient John Doe, phone +31612345678");
        oldLog.setProviderId("SwiftSend");
        oldLog.setHospitalId("HOSP-1");
        notificationLogRepository.save(oldLog);

        UUID recentEventId = UUID.randomUUID();
        NotificationLogJpaEntity recentLog = new NotificationLogJpaEntity(recentEventId);
        recentLog.setStatus("FAILED");
        recentLog.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        recentLog.setErrorMessage("Delivery failed for patient Jane Roe");
        recentLog.setProviderId("LegacyLink");
        notificationLogRepository.save(recentLog);

        scheduler.anonymizeAgedNotificationLogs();

        NotificationLogJpaEntity anonymized = notificationLogRepository.findById(oldEventId).orElseThrow();
        assertNull(anonymized.getErrorMessage(), "Patient-identifiable error detail must be stripped after 14 days");
        assertEquals("FAILED", anonymized.getStatus(), "Status meta-information must be retained");
        assertEquals("SwiftSend", anonymized.getProviderId(), "Provider name must be retained for auditing");
        assertEquals("HOSP-1", anonymized.getHospitalId(), "Hospital reference must be retained for invoice verification");

        NotificationLogJpaEntity untouched = notificationLogRepository.findById(recentEventId).orElseThrow();
        assertEquals("Delivery failed for patient Jane Roe", untouched.getErrorMessage(), "Recent logs must not be anonymized yet");
    }

    @Test
    @DisplayName("Purges notification log meta-information entirely after one year")
    void purgeExpiredMetaInformation_RemovesRecordsOlderThanOneYear() {
        UUID expiredEventId = UUID.randomUUID();
        NotificationLogJpaEntity expiredLog = new NotificationLogJpaEntity(expiredEventId);
        expiredLog.setStatus("SENT");
        expiredLog.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(400));
        notificationLogRepository.save(expiredLog);

        UUID withinAuditWindowEventId = UUID.randomUUID();
        NotificationLogJpaEntity withinAuditWindowLog = new NotificationLogJpaEntity(withinAuditWindowEventId);
        withinAuditWindowLog.setStatus("SENT");
        withinAuditWindowLog.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(200));
        notificationLogRepository.save(withinAuditWindowLog);

        scheduler.purgeExpiredMetaInformation();

        assertFalse(notificationLogRepository.findById(expiredEventId).isPresent(), "Meta-information older than 1 year must be purged");
        assertTrue(notificationLogRepository.findById(withinAuditWindowEventId).isPresent(), "Meta-information within the 1 year audit window must be kept");
    }
}
