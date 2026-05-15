package openmrsenger.restservice.appointments.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record Appointment(
    UUID id,
    String patientReference,
    LocalDateTime date,
    String status
) {}
