package openmrsenger.restservice.appointments.application;

import org.springframework.stereotype.Component;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class FhirAppointmentValidatorImpl implements FhirAppointmentValidator {

    private static final List<String> VALID_STATUSES = Arrays.asList(
            "proposed", "pending", "booked", "arrived", "fulfilled",
            "cancelled", "noshow", "entered-in-error", "checked-in", "waitlist"
    );

    private static final String PARTICIPANT_INDEX_PREFIX = "Participant at index ";

    @Override
    public List<String> validate(FhirAppointmentDto appointment) {
        List<String> errors = new ArrayList<>();

        if (appointment == null) {
            errors.add("Payload is empty or malformed");
            return errors;
        }

        validateResourceType(appointment, errors);
        validateStatus(appointment, errors);
        validateStart(appointment, errors);
        validateParticipants(appointment, errors);

        return errors;
    }

    private void validateResourceType(FhirAppointmentDto appointment, List<String> errors) {
        String resourceType = appointment.getResourceType();
        if (resourceType == null || resourceType.trim().isEmpty()) {
            errors.add("Missing mandatory field: resourceType");
        } else if (!"Appointment".equals(resourceType)) {
            errors.add("resourceType must be 'Appointment'");
        }
    }

    private void validateStatus(FhirAppointmentDto appointment, List<String> errors) {
        String status = appointment.getStatus();
        if (status == null || status.trim().isEmpty()) {
            errors.add("Missing mandatory field: status");
        } else if (!VALID_STATUSES.contains(status.toLowerCase())) {
            errors.add("Invalid status value: '" + status + "'");
        }
    }

    private void validateStart(FhirAppointmentDto appointment, List<String> errors) {
        String start = appointment.getStart();
        if (start == null || start.trim().isEmpty()) {
            errors.add("Missing mandatory field: start");
        } else {
            try {
                OffsetDateTime.parse(start);
            } catch (DateTimeParseException e) {
                errors.add("Invalid start date-time format: '" + start + "'. Expected ISO-8601 offset format.");
            }
        }
    }

    private void validateParticipants(FhirAppointmentDto appointment, List<String> errors) {
        List<ParticipantDto> participants = appointment.getParticipant();
        if (participants == null || participants.isEmpty()) {
            errors.add("Missing mandatory field: participant");
        } else {
            for (int i = 0; i < participants.size(); i++) {
                validateParticipantAt(participants.get(i), i, errors);
            }
        }
    }

    private void validateParticipantAt(ParticipantDto p, int index, List<String> errors) {
        if (p == null) {
            errors.add(PARTICIPANT_INDEX_PREFIX + index + " is null");
        } else if (p.getActor() == null) {
            errors.add(PARTICIPANT_INDEX_PREFIX + index + " is missing actor");
        } else if (p.getActor().getReference() == null || p.getActor().getReference().trim().isEmpty()) {
            errors.add(PARTICIPANT_INDEX_PREFIX + index + " actor is missing reference");
        }
    }
}

