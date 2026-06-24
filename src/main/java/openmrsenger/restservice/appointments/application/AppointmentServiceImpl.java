package openmrsenger.restservice.appointments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);
    private static final String CANCELLED_STATUS = "Cancelled";
    private static final String TOPIC = "appointment.events";

    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processWebhook(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson) {
        if (CANCELLED_STATUS.equalsIgnoreCase(dto.getStatus())) {
            handleCancellation(dto, messagingProvider, hospitalId, providerConfigJson);
        } else {
            handleSchedule(dto, messagingProvider, hospitalId, providerConfigJson);
        }
    }

    private void handleCancellation(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson) {
        UUID eventId24h = deterministicId(dto.getAppointmentUuid(), "24h");
        UUID eventId1h = deterministicId(dto.getAppointmentUuid(), "1h");

        boolean anyAlreadySent = appointmentRepository.wasNotificationSent(eventId24h)
                || appointmentRepository.wasNotificationSent(eventId1h);

        appointmentRepository.cancelOutboxMessages(List.of(eventId24h, eventId1h));
        log.info("Cancelled pending reminders for appointment {}", dto.getAppointmentUuid());

        if (anyAlreadySent) {
            String message = "CANCELLED: Your appointment at " + dto.getStartDateTime() + " at " + hospitalId + " has been cancelled.";
            sendImmediately(dto, messagingProvider, hospitalId, providerConfigJson, message);
            log.info("Sent immediate cancellation notification for appointment {}", dto.getAppointmentUuid());
        }
    }

    private void handleSchedule(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson) {
        OffsetDateTime appointmentTime = dto.getStartDateTime();
        Instant now = Instant.now();

        if (appointmentTime == null || appointmentTime.toInstant().isBefore(now)) {
            log.warn("Appointment is in the past or missing start time. Skipping reminders. Appointment ID: {}", dto.getAppointmentUuid());
            return;
        }

        UUID eventId24h = deterministicId(dto.getAppointmentUuid(), "24h");
        boolean sent24h = appointmentRepository.wasNotificationSent(eventId24h);

        scheduleReminder(dto, messagingProvider, hospitalId, providerConfigJson, appointmentTime.minusHours(24).toInstant(), "24h");
        scheduleReminder(dto, messagingProvider, hospitalId, providerConfigJson, appointmentTime.minusHours(1).toInstant(), "1h");

        if (sent24h) {
            String message = "UPDATED: Your appointment is now scheduled for " + dto.getStartDateTime() + " at " + hospitalId + ".";
            sendImmediately(dto, messagingProvider, hospitalId, providerConfigJson, message);
            log.info("Sent immediate update notification for appointment {} (24h reminder was already sent)", dto.getAppointmentUuid());
        }
    }

    private void scheduleReminder(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson, Instant scheduledFor, String suffix) {
        Instant now = Instant.now();
        if (scheduledFor.isBefore(now)) {
            log.info("Reminder {} for appointment {} is in the past ({}). Skipping.", suffix, dto.getAppointmentUuid(), scheduledFor);
            return;
        }

        UUID eventId = deterministicId(dto.getAppointmentUuid(), suffix);
        // If this reminder was already sent, use a fresh payload UUID so the listener
        // doesn't dedup it as a duplicate of the previously-delivered notification.
        boolean alreadySent = appointmentRepository.wasNotificationSent(eventId);
        UUID payloadEventId = alreadySent ? UUID.randomUUID() : eventId;
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                payloadEventId,
                null,
                dto.getPatientUuid(),
                dto.getPhoneNumber(),
                "Reminder (" + suffix + "): Appointment at " + dto.getStartDateTime() + " - " + hospitalId,
                messagingProvider.toUpperCase(),
                hospitalId,
                providerConfigJson
        );

        saveEvent(event, scheduledFor, scheduledFor.plus(2, ChronoUnit.HOURS), eventId);
        log.info("Scheduled {} reminder for appointment {} at {}", suffix, dto.getAppointmentUuid(), scheduledFor);
    }

    private void sendImmediately(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson, String messageText) {
        Instant now = Instant.now();
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                null,
                null,
                dto.getPatientUuid(),
                dto.getPhoneNumber(),
                messageText,
                messagingProvider.toUpperCase(),
                hospitalId,
                providerConfigJson
        );
        // Random UUID (no deterministic ID) — immediate notifications are fire-and-forget
        saveEvent(event, now, now.plus(1, ChronoUnit.HOURS), null);
    }

    private void saveEvent(NotificationRequestedEvent event, Instant scheduledFor, Instant expiresAt, UUID eventId) {
        try {
            appointmentRepository.saveToOutbox(TOPIC, objectMapper.writeValueAsString(event), scheduledFor, expiresAt, eventId);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize NotificationRequestedEvent", e);
        }
    }

    private static UUID deterministicId(String appointmentUuid, String suffix) {
        return UUID.nameUUIDFromBytes((appointmentUuid + "-" + suffix).getBytes(StandardCharsets.UTF_8));
    }
}
