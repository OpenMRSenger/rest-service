package openmrsenger.restservice.appointments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentServiceImpl.class);
    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processWebhook(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson) {
        LocalDateTime appointmentTime = dto.getStartDateTime();
        LocalDateTime now = LocalDateTime.now();

        if (appointmentTime == null || appointmentTime.isBefore(now)) {
            log.warn("Appointment is in the past or missing start time. Skipping reminders. Appointment ID: {}", dto.getAppointmentUuid());
            return;
        }

        // Schedule 24h reminder
        scheduleReminder(dto, messagingProvider, hospitalId, providerConfigJson, appointmentTime.minusHours(24), "24h");
        
        // Schedule 1h reminder
        scheduleReminder(dto, messagingProvider, hospitalId, providerConfigJson, appointmentTime.minusHours(1), "1h");
    }

    private void scheduleReminder(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson, LocalDateTime scheduledFor, String suffix) {
        LocalDateTime now = LocalDateTime.now();
        if (scheduledFor.isBefore(now)) {
            log.info("Reminder {} for appointment {} is in the past ({}). Skipping.", suffix, dto.getAppointmentUuid(), scheduledFor);
            return;
        }

        try {
            UUID eventId = UUID.nameUUIDFromBytes((dto.getAppointmentUuid() + "-" + suffix).getBytes(StandardCharsets.UTF_8));
            
            NotificationRequestedEvent event = new NotificationRequestedEvent(
                    eventId,
                    null,
                    dto.getPatientUuid(),
                    dto.getPhoneNumber(),
                    "Reminder (" + suffix + "): Appointment at " + dto.getStartDateTime() + " - " + hospitalId,
                    messagingProvider.toUpperCase(),
                    hospitalId,
                    providerConfigJson
            );

            // Set expiration to 2 hours after scheduled time
            LocalDateTime expiresAt = scheduledFor.plusHours(2);
            
            appointmentRepository.saveToOutbox("appointment.events", objectMapper.writeValueAsString(event), scheduledFor, expiresAt);
            log.info("Scheduled {} reminder for appointment {} at {}", suffix, dto.getAppointmentUuid(), scheduledFor);

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize NotificationRequestedEvent", e);
        }
    }
}