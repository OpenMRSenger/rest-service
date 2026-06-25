package openmrsenger.restservice.retention;

import openmrsenger.restservice.appointments.infrastructure.persistence.SpringDataAppointmentRepository;
import openmrsenger.restservice.appointments.infrastructure.persistence.SpringDataOutboxRepository;
import openmrsenger.restservice.communications.infrastructure.persistence.SpringDataNotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Enforces GDPR data retention: appointments and outbox messages (which carry
 * patient-identifiable payloads) are purged 14 days after creation. Notification log
 * error detail, which may leak patient data, is stripped after the same 14 days, leaving
 * only status/timestamp/provider meta-information for auditing and invoice verification.
 * That meta-information is purged entirely after one year.
 */
@Component
public class DataRetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(DataRetentionScheduler.class);

    private final SpringDataAppointmentRepository appointmentRepository;
    private final SpringDataOutboxRepository outboxRepository;
    private final SpringDataNotificationLogRepository notificationLogRepository;
    private final int identifiableDataRetentionDays;
    private final int metaInformationRetentionDays;

    public DataRetentionScheduler(
            SpringDataAppointmentRepository appointmentRepository,
            SpringDataOutboxRepository outboxRepository,
            SpringDataNotificationLogRepository notificationLogRepository,
            @Value("${retention.identifiable-data.days:14}") int identifiableDataRetentionDays,
            @Value("${retention.meta-information.days:365}") int metaInformationRetentionDays) {
        this.appointmentRepository = appointmentRepository;
        this.outboxRepository = outboxRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.identifiableDataRetentionDays = identifiableDataRetentionDays;
        this.metaInformationRetentionDays = metaInformationRetentionDays;
    }

    @Scheduled(cron = "${retention.cleanup.cron:0 0 2 * * *}")
    @Transactional
    public void runDataRetention() {
        purgeIdentifiableData();
        anonymizeAgedNotificationLogs();
        purgeExpiredMetaInformation();
    }

    @Transactional
    public void purgeIdentifiableData() {
        OffsetDateTime outboxCutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(identifiableDataRetentionDays);
        LocalDateTime appointmentCutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(identifiableDataRetentionDays);

        int deletedOutboxMessages = outboxRepository.deleteByCreatedAtBefore(outboxCutoff);
        int deletedAppointments = appointmentRepository.deleteByDateBefore(appointmentCutoff);

        log.info("Data retention: purged {} outbox messages and {} appointments older than {} days",
                deletedOutboxMessages, deletedAppointments, identifiableDataRetentionDays);
    }

    @Transactional
    public void anonymizeAgedNotificationLogs() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(identifiableDataRetentionDays);
        int anonymized = notificationLogRepository.anonymizeOlderThan(cutoff);

        log.info("Data retention: anonymized {} notification logs older than {} days", anonymized, identifiableDataRetentionDays);
    }

    @Transactional
    public void purgeExpiredMetaInformation() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC).minusDays(metaInformationRetentionDays);
        int deleted = notificationLogRepository.deleteByCreatedAtBefore(cutoff);

        log.info("Data retention: purged {} notification log meta-information records older than {} days",
                deleted, metaInformationRetentionDays);
    }
}
