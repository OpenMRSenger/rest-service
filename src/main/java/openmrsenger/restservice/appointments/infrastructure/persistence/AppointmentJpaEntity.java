package openmrsenger.restservice.appointments.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class AppointmentJpaEntity {

    @Id
    private UUID id;

    @Column(name = "patient_reference", nullable = false)
    private String patientReference;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime date;

    @Column(name = "status", nullable = false)
    private String status;

    protected AppointmentJpaEntity() {}

    public AppointmentJpaEntity(UUID id, String patientReference, LocalDateTime date, String status) {
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
