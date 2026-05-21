package openmrsenger.restservice.appointments.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import openmrsenger.restservice.appointments.application.AppointmentService;
import openmrsenger.restservice.appointments.application.OpenMrsWebhookDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AppointmentWebhookController.class);

    private final AppointmentService appointmentService;
    private final WebhookAuthenticator authenticator;

    public AppointmentWebhookController(AppointmentService appointmentService, WebhookAuthenticator authenticator) {
        this.appointmentService = appointmentService;
        this.authenticator = authenticator;
    }

    @PostMapping
    public ResponseEntity<String> receiveAppointment(
            @RequestBody OpenMrsWebhookDto dto,
            @RequestHeader("x-messaging-provider") String messagingProvider,
            @RequestHeader("x-hospital-name") String hospitalName,
            @RequestHeader(value = "x-provider-config", required = false) String providerConfigJson,
            HttpServletRequest request) {

        if (!authenticator.authenticate(request)) {
            log.warn("Unauthorized webhook attempt from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Invalid or missing bearer token.");
        }

        log.info("Received appointment webhook for patient: {}, provider: {}, messaging provider: {}, hospital: {}",
                dto.getPatientUuid(), dto.getArtsName(), messagingProvider, hospitalName);

        // We sturen de rauwe configuratie JSON-string als een dom doorgeefluik direct door
        appointmentService.processWebhook(dto, messagingProvider, hospitalName, providerConfigJson);

        log.info("Appointment webhook processed and added to outbox for patient: {}", dto.getPatientUuid());
        return ResponseEntity.ok("Appointment webhook received and added to outbox.");
    }
}