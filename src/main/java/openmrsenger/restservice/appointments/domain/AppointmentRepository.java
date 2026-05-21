package openmrsenger.restservice.appointments.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {
    void save(Appointment appointment, String eventPayload);

    void saveAppointment(Appointment appointment);

    Optional<Appointment> findAppointmentById(UUID id);

    void saveToOutbox(String topic, String payload);

    void saveToOutbox(String topic, String payload, LocalDateTime scheduledFor, LocalDateTime expiresAt, UUID eventId);

    void cancelOutboxMessages(List<UUID> eventIds);

    boolean wasNotificationSent(UUID eventId);
}
