package openmrsenger.restservice.appointments.infrastructure.web.fhir;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FhirAppointmentValidator {

    private static final Set<String> VALID_STATUSES = Set.of(
            "proposed", "pending", "booked", "arrived", "fulfilled",
            "cancelled", "noshow", "entered-in-error", "checked-in", "waitlist"
    );

    public List<OperationOutcomeDto.IssueDto> validate(FhirAppointmentDto dto) {
        List<OperationOutcomeDto.IssueDto> issues = new ArrayList<>();

        // 1. resourceType validation
        if (dto.getResourceType() == null || dto.getResourceType().trim().isEmpty()) {
            issues.add(new OperationOutcomeDto.IssueDto(
                    "error",
                    "required",
                    "resourceType is required",
                    List.of("Appointment.resourceType")
            ));
        } else if (!"Appointment".equals(dto.getResourceType())) {
            issues.add(new OperationOutcomeDto.IssueDto(
                    "error",
                    "invalid",
                    "resourceType must be 'Appointment'",
                    List.of("Appointment.resourceType")
            ));
        }

        // 2. status validation
        if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            issues.add(new OperationOutcomeDto.IssueDto(
                    "error",
                    "required",
                    "status is required",
                    List.of("Appointment.status")
            ));
        } else if (!VALID_STATUSES.contains(dto.getStatus())) {
            issues.add(new OperationOutcomeDto.IssueDto(
                    "error",
                    "invalid",
                    "Invalid status value: '" + dto.getStatus() + "'",
                    List.of("Appointment.status")
            ));
        }

        // 3. start validation
        if (dto.getStart() == null || dto.getStart().trim().isEmpty()) {
            issues.add(new OperationOutcomeDto.IssueDto(
                    "error",
                    "required",
                    "start date-time is required",
                    List.of("Appointment.start")
            ));
        } else {
            try {
                OffsetDateTime.parse(dto.getStart());
            } catch (DateTimeParseException e) {
                issues.add(new OperationOutcomeDto.IssueDto(
                        "error",
                        "invalid",
                        "Invalid start date-time format: '" + dto.getStart() + "'",
                        List.of("Appointment.start")
                ));
            }
        }

        // 4. participant validation
        if (dto.getParticipant() == null || dto.getParticipant().isEmpty()) {
            issues.add(new OperationOutcomeDto.IssueDto(
                    "error",
                    "required",
                    "participant list is required and must not be empty",
                    List.of("Appointment.participant")
            ));
        } else {
            for (int i = 0; i < dto.getParticipant().size(); i++) {
                FhirAppointmentDto.ParticipantDto p = dto.getParticipant().get(i);
                if (p == null || p.getActor() == null) {
                    issues.add(new OperationOutcomeDto.IssueDto(
                            "error",
                            "required",
                            "actor is required for participant at index " + i,
                            List.of("Appointment.participant[" + i + "].actor")
                    ));
                }
            }
        }

        return issues;
    }
}
