package openmrsenger.restservice.appointments.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import openmrsenger.restservice.appointments.application.AppointmentServiceImpl;
import openmrsenger.restservice.appointments.application.OpenMrsWebhookDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/appointments")
public class AppointmentWebhookController {

    private final AppointmentServiceImpl appointmentService;
    private final WebhookAuthenticator authenticator;

    public AppointmentWebhookController(AppointmentServiceImpl appointmentService, WebhookAuthenticator authenticator) {
        this.appointmentService = appointmentService;
        this.authenticator = authenticator;
    }

    @PostMapping
    public ResponseEntity<String> receiveAppointment(
            @RequestBody OpenMrsWebhookDto dto,
            @RequestHeader("x-messaging-provider") String messagingProvider,
            HttpServletRequest request) {

        if (!authenticator.authenticate(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid secret key.");
        }

        appointmentService.processWebhook(dto, messagingProvider);

        return ResponseEntity.ok("Appointment webhook received and added to outbox.");
    }
}
