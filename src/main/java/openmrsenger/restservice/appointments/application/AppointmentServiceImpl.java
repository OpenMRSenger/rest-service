package openmrsenger.restservice.appointments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentServiceImpl {

    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processWebhook(OpenMrsWebhookDto dto, String messagingProvider) {
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                dto.getPatientUuid(),
                dto.getPhoneNumber(),
                "Appointment update: " + dto.getStatus() + " (Start: " + dto.getStartDateTime() + ")",
                messagingProvider
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            appointmentRepository.saveToOutbox("appointment.events", payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize NotificationRequestedEvent", e);
        }
    }
}
