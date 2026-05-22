package openmrsenger.restservice.appointments.domain;

import java.time.OffsetDateTime;

public interface AppointmentRepository {
    /**
     * Saves the appointment and the outbox event payload in a single transaction.
     */
    void save(Appointment appointment, String eventPayload);

    /**
     * Saves a payload directly to the outbox topic.
     */
    void saveToOutbox(String topic, String payload);

    /**
     * Saves a payload to the outbox with scheduling information.
     */
    void saveToOutbox(String topic, String payload, OffsetDateTime scheduledFor, OffsetDateTime expiresAt);
}
