package openmrsenger.restservice.appointments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processWebhook(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson) {
        try {
            NotificationRequestedEvent event = new NotificationRequestedEvent(
                    dto.getPatientUuid(),
                    dto.getPhoneNumber(),
                    "Appointment update: " + dto.getStatus() + " (Start: " + dto.getStartDateTime() + ") - " + hospitalId,
                    messagingProvider.toUpperCase(),
                    hospitalId,
                    providerConfigJson
            );
            appointmentRepository.saveToOutbox("appointment.events", objectMapper.writeValueAsString(event));

        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize NotificationRequestedEvent", e);
        }
    }
}