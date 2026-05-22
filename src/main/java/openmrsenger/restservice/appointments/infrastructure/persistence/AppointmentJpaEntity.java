package openmrsenger.restservice.appointments.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class AppointmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "patient_reference", nullable = false)
    private String patientReference;

    @Column(name = "appointment_date", nullable = false)
    private OffsetDateTime date;

    @Column(name = "status", nullable = false)
    private String status;

    protected AppointmentJpaEntity() {}

    public AppointmentJpaEntity(UUID id, String patientReference, OffsetDateTime date, String status) {
        this.id = id;
        this.patientReference = patientReference;
        this.date = date;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getPatientReference() { return patientReference; }
    public OffsetDateTime getDate() { return date; }
    public String getStatus() { return status; }
}
