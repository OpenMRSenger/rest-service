package openmrsenger.restservice.appointments.application;

import java.util.List;

public interface FhirAppointmentValidator {
    List<String> validate(FhirAppointmentDto appointment);
}
