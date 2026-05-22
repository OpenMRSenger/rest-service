package openmrsenger.restservice.appointments.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Appointment(
    UUID id,
    String patientReference,
    OffsetDateTime date,
    String status
) {}
