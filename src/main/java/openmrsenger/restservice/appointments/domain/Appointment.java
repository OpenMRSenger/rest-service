package openmrsenger.restservice.appointments.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Appointment {
    private final UUID id;
    private final String patientReference;
    private final LocalDateTime date;
    private final String status;

    public Appointment(UUID id, String patientReference, LocalDateTime date, String status) {
        this.id = id;
        this.patientReference = patientReference;
        this.date = date;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getPatientReference() { return patientReference; }
    public LocalDateTime getDate() { return date; }
    public String getStatus() { return status; }
}
