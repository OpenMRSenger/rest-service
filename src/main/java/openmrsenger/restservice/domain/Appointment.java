package openmrsenger.restservice.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_reference", nullable = false)
    private String patientReference;

    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime date;

    @Column(name = "status", nullable = false)
    private String status;

    protected Appointment() {}

    public Appointment(String patientReference, LocalDateTime date, String status) {
        this.patientReference = patientReference;
        this.date = date;
        this.status = status;
    }

    public UUID getId() { return id; }
    public String getPatientReference() { return patientReference; }
    public LocalDateTime getDate() { return date; }
    public String getStatus() { return status; }

    public void setPatientReference(String patientReference) { this.patientReference = patientReference; }
    public void setDate(LocalDateTime date) { this.date = date; }
    public void setStatus(String status) { this.status = status; }
}