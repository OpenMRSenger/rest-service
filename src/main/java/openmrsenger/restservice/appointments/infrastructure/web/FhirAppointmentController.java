package openmrsenger.restservice.appointments.infrastructure.web;

import openmrsenger.restservice.appointments.application.AppointmentServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/fhir/Appointment")
public class FhirAppointmentController {

    private final AppointmentServiceImpl appointmentService;

    public FhirAppointmentController(AppointmentServiceImpl appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<String> receiveAppointment(@RequestBody FhirAppointmentDto dto) {
        appointmentService.scheduleAppointment(dto.getPatientReference(), dto.getStart(), dto.getStatus());
        return ResponseEntity.ok("Appointment received and processed.");
    }
}
