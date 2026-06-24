package openmrsenger.restservice.appointments.infrastructure.web.fhir;

import openmrsenger.restservice.appointments.application.ActorDto;
import openmrsenger.restservice.appointments.application.FhirAppointmentDto;
import openmrsenger.restservice.appointments.application.OperationOutcomeDto;
import openmrsenger.restservice.appointments.application.ParticipantDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FhirAppointmentValidatorTest {

    private FhirAppointmentValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FhirAppointmentValidator();
    }

    @Test
    void validate_WithValidPayload_ShouldReturnNoIssues() {
        FhirAppointmentDto dto = createValidDto();

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertTrue(issues.isEmpty());
    }

    @Test
    void validate_WithMissingResourceType_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.setResourceType(null);

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("required", issues.get(0).getCode());
        assertEquals("resourceType is required", issues.get(0).getDiagnostics());
    }

    @Test
    void validate_WithInvalidResourceType_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.setResourceType("Observation");

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("invalid", issues.get(0).getCode());
        assertEquals("resourceType must be 'Appointment'", issues.get(0).getDiagnostics());
    }

    @Test
    void validate_WithMissingStatus_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.setStatus("  ");

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("required", issues.get(0).getCode());
        assertEquals("status is required", issues.get(0).getDiagnostics());
    }

    @Test
    void validate_WithInvalidStatus_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.setStatus("some-random-status");

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("invalid", issues.get(0).getCode());
        assertTrue(issues.get(0).getDiagnostics().contains("Invalid status value"));
    }

    @Test
    void validate_WithMissingStart_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.setStart(null);

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("required", issues.get(0).getCode());
        assertEquals("start date-time is required", issues.get(0).getDiagnostics());
    }

    @Test
    void validate_WithInvalidStartFormat_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.setStart("2026-06-24 12:00:00"); // Missing T and offset

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("invalid", issues.get(0).getCode());
        assertTrue(issues.get(0).getDiagnostics().contains("Invalid start date-time format"));
    }

    @Test
    void validate_WithMissingParticipant_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.setParticipant(null);

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("required", issues.get(0).getCode());
        assertEquals("participant list is required and must not be empty", issues.get(0).getDiagnostics());
    }

    @Test
    void validate_WithEmptyParticipantList_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.setParticipant(Collections.emptyList());

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("required", issues.get(0).getCode());
        assertEquals("participant list is required and must not be empty", issues.get(0).getDiagnostics());
    }

    @Test
    void validate_WithParticipantMissingActor_ShouldReturnIssue() {
        FhirAppointmentDto dto = createValidDto();
        dto.getParticipant().get(0).setActor(null);

        List<OperationOutcomeDto.IssueDto> issues = validator.validate(dto);

        assertEquals(1, issues.size());
        assertEquals("error", issues.get(0).getSeverity());
        assertEquals("required", issues.get(0).getCode());
        assertEquals("actor is required for participant at index 0", issues.get(0).getDiagnostics());
    }

    private FhirAppointmentDto createValidDto() {
        FhirAppointmentDto dto = new FhirAppointmentDto();
        dto.setResourceType("Appointment");
        dto.setId("app-uuid");
        dto.setStatus("booked");
        dto.setStart("2026-06-24T12:00:00Z");

        ParticipantDto participant = new ParticipantDto();
        ActorDto actor = new ActorDto();
        actor.setReference("Patient/patient-uuid");
        actor.setDisplay("John Doe");
        participant.setActor(actor);

        List<ParticipantDto> participants = new ArrayList<>();
        participants.add(participant);
        dto.setParticipant(participants);

        return dto;
    }
}
