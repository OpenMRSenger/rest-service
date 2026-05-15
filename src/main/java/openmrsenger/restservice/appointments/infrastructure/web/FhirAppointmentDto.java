package openmrsenger.restservice.appointments.infrastructure.web;

import java.time.LocalDateTime;

public class FhirAppointmentDto {
    private String patientReference;
    private LocalDateTime start;
    private String status;

    public String getPatientReference() {
        return patientReference;
    }

    public void setPatientReference(String patientReference) {
        this.patientReference = patientReference;
    }

    public LocalDateTime getStart() {
        return start;
    }

    public void setStart(LocalDateTime start) {
        this.start = start;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
