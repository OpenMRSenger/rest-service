package openmrsenger.restservice.appointments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    @Value("${student.group}")
    private String studentGroup;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processWebhook(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, WebhookCredentials credentials) {
        Map<String, String> configMap = new LinkedHashMap<>();
        configMap.put("studentGroup", studentGroup);
        if (credentials.token() != null)        configMap.put("apiKey", credentials.token());
        if (credentials.username() != null)     configMap.put("username", credentials.username());
        if (credentials.password() != null)     configMap.put("password", credentials.password());
        if (credentials.clientId() != null)     configMap.put("clientId", credentials.clientId());
        if (credentials.clientSecret() != null) configMap.put("clientSecret", credentials.clientSecret());

        try {
            String configJson = objectMapper.writeValueAsString(configMap);
            NotificationRequestedEvent event = new NotificationRequestedEvent(
                    dto.getPatientUuid(),
                    dto.getPhoneNumber(),
                    "Appointment update: " + dto.getStatus() + " (Start: " + dto.getStartDateTime() + ") - " + hospitalId,
                    messagingProvider.toUpperCase(),
                    hospitalId,
                    configJson
            );
            appointmentRepository.saveToOutbox("appointment.events", objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event", e);
        }
    }
}
