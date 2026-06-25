package openmrsenger.restservice.appointments.infrastructure.web.fhir;

import openmrsenger.restservice.appointments.application.FhirAppointmentDto;
import openmrsenger.restservice.appointments.application.OperationOutcomeDto;
import java.util.List;

public interface FhirValidationRule {
    void validate(FhirAppointmentDto dto, List<OperationOutcomeDto.IssueDto> issues);
}
