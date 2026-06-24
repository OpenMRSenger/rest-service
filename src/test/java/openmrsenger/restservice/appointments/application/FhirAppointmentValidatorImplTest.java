package openmrsenger.restservice.appointments.application;

import org.junit.jupiter.api.Test;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FhirAppointmentValidatorImplTest {

    private final FhirAppointmentValidatorImpl validator = new FhirAppointmentValidatorImpl();

    @Test
    void validate_WithNullPayload_ReturnsError() {
        List<String> errors = validator.validate(null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Payload is empty or malformed"));
    }

    @Test
    void validate_WithValidPayload_ReturnsNoErrors() {
        FhirAppointmentDto dto = createValidDto();
        List<String> errors = validator.validate(dto);
        assertTrue(errors.isEmpty(), "Valid payload should have no errors: " + errors);
    }

    @Test
    void validate_MissingResourceType_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        dto.setResourceType(null);
        List<String> errors = validator.validate(dto);
        assertTrue(errors.contains("Missing mandatory field: resourceType"));

        dto.setResourceType("   ");
        errors = validator.validate(dto);
        assertTrue(errors.contains("Missing mandatory field: resourceType"));
    }

    @Test
    void validate_InvalidResourceType_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        dto.setResourceType("Patient");
        List<String> errors = validator.validate(dto);
        assertTrue(errors.contains("resourceType must be 'Appointment'"));
    }

    @Test
    void validate_MissingStatus_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        dto.setStatus(null);
        List<String> errors = validator.validate(dto);
        assertTrue(errors.contains("Missing mandatory field: status"));

        dto.setStatus("  ");
        errors = validator.validate(dto);
        assertTrue(errors.contains("Missing mandatory field: status"));
    }

    @Test
    void validate_InvalidStatus_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        dto.setStatus("invalid-status-here");
        List<String> errors = validator.validate(dto);
        assertTrue(errors.contains("Invalid status value: 'invalid-status-here'"));
    }

    @Test
    void validate_MissingStart_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        dto.setStart(null);
        List<String> errors = validator.validate(dto);
        assertTrue(errors.contains("Missing mandatory field: start"));
    }

    @Test
    void validate_InvalidStartDateFormat_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        dto.setStart("2026-06-24 12:00:00");
        List<String> errors = validator.validate(dto);
        assertTrue(errors.stream().anyMatch(e -> e.startsWith("Invalid start date-time format")));
    }

    @Test
    void validate_MissingParticipant_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        dto.setParticipant(null);
        List<String> errors = validator.validate(dto);
        assertTrue(errors.contains("Missing mandatory field: participant"));

        dto.setParticipant(Collections.emptyList());
        errors = validator.validate(dto);
        assertTrue(errors.contains("Missing mandatory field: participant"));
    }

    @Test
    void validate_ParticipantMissingActor_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        ParticipantDto p = new ParticipantDto();
        p.setActor(null);
        dto.setParticipant(List.of(p));
        
        List<String> errors = validator.validate(dto);
        assertTrue(errors.contains("Participant at index 0 is missing actor"));
    }

    @Test
    void validate_ParticipantActorMissingReference_ReturnsError() {
        FhirAppointmentDto dto = createValidDto();
        ParticipantDto p = new ParticipantDto();
        ActorDto actor = new ActorDto();
        actor.setReference(null);
        p.setActor(actor);
        dto.setParticipant(List.of(p));
        
        List<String> errors = validator.validate(dto);
        assertTrue(errors.contains("Participant at index 0 actor is missing reference"));
    }

    private FhirAppointmentDto createValidDto() {
        FhirAppointmentDto dto = new FhirAppointmentDto();
        dto.setResourceType("Appointment");
        dto.setStatus("booked");
        dto.setStart("2026-06-24T12:00:00+02:00");

        TelecomDto telecom = new TelecomDto("phone", "+31612345678");
        ActorDto actor = new ActorDto("Patient/patient-uuid", "John Doe", List.of(telecom));
        ParticipantDto participant = new ParticipantDto(actor, "accepted");
        dto.setParticipant(List.of(participant));

        return dto;
    }
}
