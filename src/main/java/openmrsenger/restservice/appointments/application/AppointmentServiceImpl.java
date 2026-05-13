package openmrsenger.restservice.appointments.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.domain.Appointment;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AppointmentServiceImpl {

    private final AppointmentRepository appointmentRepository;
    private final ObjectMapper objectMapper;

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, ObjectMapper objectMapper) {
        this.appointmentRepository = appointmentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void scheduleAppointment(String patientReference, LocalDateTime date, String status) {
        Appointment appointment = new Appointment(UUID.randomUUID(), patientReference, date, status);
        
        // Construct the Notification Event. In a real scenario, we might look up patient details here.
        // For now, we simulate finding a phone number and a provider.
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                patientReference,
                "+31612345678", // Example number
                "Your appointment is scheduled for " + date.toString(),
                "SWIFTSEND" // Example provider
        );

        String eventPayload;
        try {
            eventPayload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize NotificationRequestedEvent", e);
        }

        appointmentRepository.save(appointment, eventPayload);
    }
}
