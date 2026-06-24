package openmrsenger.restservice.appointments.infrastructure.web.fhir;

import java.util.List;

public interface FhirValidationRule {
    void validate(FhirAppointmentDto dto, List<OperationOutcomeDto.IssueDto> issues);
}
