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

    @Override
    public List<String> validate(FhirAppointmentDto appointment) {
        List<String> errors = new ArrayList<>();

        if (appointment == null) {
            errors.add("Payload is empty or malformed");
            return errors;
        }

        // 1. resourceType validation
        if (appointment.getResourceType() == null || appointment.getResourceType().trim().isEmpty()) {
            errors.add("Missing mandatory field: resourceType");
        } else if (!"Appointment".equals(appointment.getResourceType())) {
            errors.add("resourceType must be 'Appointment'");
        }

        // 2. status validation
        if (appointment.getStatus() == null || appointment.getStatus().trim().isEmpty()) {
            errors.add("Missing mandatory field: status");
        } else if (!VALID_STATUSES.contains(appointment.getStatus().toLowerCase())) {
            errors.add("Invalid status value: '" + appointment.getStatus() + "'");
        }

        // 3. start validation
        if (appointment.getStart() == null || appointment.getStart().trim().isEmpty()) {
            errors.add("Missing mandatory field: start");
        } else {
            try {
                OffsetDateTime.parse(appointment.getStart());
            } catch (DateTimeParseException e) {
                errors.add("Invalid start date-time format: '" + appointment.getStart() + "'. Expected ISO-8601 offset format.");
            }
        }

        // 4. participant validation
        if (appointment.getParticipant() == null || appointment.getParticipant().isEmpty()) {
            errors.add("Missing mandatory field: participant");
        } else {
            for (int i = 0; i < appointment.getParticipant().size(); i++) {
                ParticipantDto p = appointment.getParticipant().get(i);
                if (p == null) {
                    errors.add("Participant at index " + i + " is null");
                } else if (p.getActor() == null) {
                    errors.add("Participant at index " + i + " is missing actor");
                } else if (p.getActor().getReference() == null || p.getActor().getReference().trim().isEmpty()) {
                    errors.add("Participant at index " + i + " actor is missing reference");
                }
            }
        }

        return errors;
    }
}
