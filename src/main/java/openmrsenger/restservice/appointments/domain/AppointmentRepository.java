package openmrsenger.restservice.appointments.domain;

public interface AppointmentRepository {
    /**
     * Saves the appointment and the outbox event payload in a single transaction.
     */
    void save(Appointment appointment, String eventPayload);
}
