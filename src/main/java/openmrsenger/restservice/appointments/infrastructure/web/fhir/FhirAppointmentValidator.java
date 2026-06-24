package openmrsenger.restservice.appointments.infrastructure.web.fhir;

import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
public class FhirAppointmentValidator {

    private static final String SEVERITY_ERROR = "error";
    private static final String CODE_REQUIRED = "required";
    private static final String CODE_INVALID = "invalid";

    private static final Set<String> VALID_STATUSES = Set.of(
            "proposed", "pending", "booked", "arrived", "fulfilled",
            "cancelled", "noshow", "entered-in-error", "checked-in", "waitlist"
    );

    private final List<FhirValidationRule> rules = List.of(
            new ResourceTypeRule(),
            new StatusRule(),
            new StartDateTimeRule(),
            new ParticipantRule()
    );

    public List<OperationOutcomeDto.IssueDto> validate(FhirAppointmentDto dto) {
        List<OperationOutcomeDto.IssueDto> issues = new ArrayList<>();
        if (dto != null) {
            for (FhirValidationRule rule : rules) {
                rule.validate(dto, issues);
            }
        }
        return issues;
    }



    private static class ResourceTypeRule implements FhirValidationRule {
        @Override
        public void validate(FhirAppointmentDto dto, List<OperationOutcomeDto.IssueDto> issues) {
            if (dto.getResourceType() == null || dto.getResourceType().trim().isEmpty()) {
                issues.add(new OperationOutcomeDto.IssueDto(
                        SEVERITY_ERROR,
                        CODE_REQUIRED,
                        "resourceType is required",
                        List.of("Appointment.resourceType")
                ));
            } else if (!"Appointment".equals(dto.getResourceType())) {
                issues.add(new OperationOutcomeDto.IssueDto(
                        SEVERITY_ERROR,
                        CODE_INVALID,
                        "resourceType must be 'Appointment'",
                        List.of("Appointment.resourceType")
                ));
            }
        }
    }

    private static class StatusRule implements FhirValidationRule {
        @Override
        public void validate(FhirAppointmentDto dto, List<OperationOutcomeDto.IssueDto> issues) {
            if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
                issues.add(new OperationOutcomeDto.IssueDto(
                        SEVERITY_ERROR,
                        CODE_REQUIRED,
                        "status is required",
                        List.of("Appointment.status")
                ));
            } else if (!VALID_STATUSES.contains(dto.getStatus())) {
                issues.add(new OperationOutcomeDto.IssueDto(
                        SEVERITY_ERROR,
                        CODE_INVALID,
                        "Invalid status value: '" + dto.getStatus() + "'",
                        List.of("Appointment.status")
                ));
            }
        }
    }

    private static class StartDateTimeRule implements FhirValidationRule {
        @Override
        public void validate(FhirAppointmentDto dto, List<OperationOutcomeDto.IssueDto> issues) {
            if (dto.getStart() == null || dto.getStart().trim().isEmpty()) {
                issues.add(new OperationOutcomeDto.IssueDto(
                        SEVERITY_ERROR,
                        CODE_REQUIRED,
                        "start date-time is required",
                        List.of("Appointment.start")
                ));
            } else {
                try {
                    OffsetDateTime.parse(dto.getStart());
                } catch (DateTimeParseException e) {
                    issues.add(new OperationOutcomeDto.IssueDto(
                            SEVERITY_ERROR,
                            CODE_INVALID,
                            "Invalid start date-time format: '" + dto.getStart() + "'",
                            List.of("Appointment.start")
                    ));
                }
            }
        }
    }

    private static class ParticipantRule implements FhirValidationRule {
        @Override
        public void validate(FhirAppointmentDto dto, List<OperationOutcomeDto.IssueDto> issues) {
            if (dto.getParticipant() == null || dto.getParticipant().isEmpty()) {
                issues.add(new OperationOutcomeDto.IssueDto(
                        SEVERITY_ERROR,
                        CODE_REQUIRED,
                        "participant list is required and must not be empty",
                        List.of("Appointment.participant")
                ));
            } else {
                int i = 0;
                for (FhirAppointmentDto.ParticipantDto p : dto.getParticipant()) {
                    if (p == null || p.getActor() == null) {
                        issues.add(new OperationOutcomeDto.IssueDto(
                                SEVERITY_ERROR,
                                CODE_REQUIRED,
                                "actor is required for participant at index " + i,
                                List.of("Appointment.participant[" + i + "].actor")
                        ));
                    }
                    i++;
                }
            }
        }
    }
}
