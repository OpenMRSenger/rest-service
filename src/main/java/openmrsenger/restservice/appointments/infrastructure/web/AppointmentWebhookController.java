package openmrsenger.restservice.appointments.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import openmrsenger.restservice.appointments.application.AppointmentService;
import openmrsenger.restservice.appointments.application.FhirAppointmentDto;
import openmrsenger.restservice.appointments.application.OperationOutcomeDto;
import openmrsenger.restservice.appointments.infrastructure.web.fhir.FhirAppointmentValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/webhooks/appointments")
public class AppointmentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentWebhookController.class);

    private final AppointmentService appointmentService;
    private final WebhookAuthenticator authenticator;
    private final ObjectMapper objectMapper;
    private final FhirAppointmentValidator validator;

    public AppointmentWebhookController(AppointmentService appointmentService,
                                         WebhookAuthenticator authenticator,
                                         ObjectMapper objectMapper,
                                         FhirAppointmentValidator validator) {
        this.appointmentService = appointmentService;
        this.authenticator = authenticator;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<OperationOutcomeDto> receiveAppointment(
            @RequestBody String rawPayload,
            @RequestHeader("x-messaging-provider") String messagingProvider,
            @RequestHeader("x-hospital-name") String hospitalName,
            @RequestHeader(value = "x-provider-config", required = false) String providerConfigJson,
            HttpServletRequest request) {

        if (!authenticator.authenticate(request)) {
            log.warn("Unauthorized webhook attempt from IP: {}", request.getRemoteAddr());
            OperationOutcomeDto authOutcome = new OperationOutcomeDto(
                    "fatal", "security", "Unauthorized: Invalid or missing bearer token."
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(authOutcome);
        }

        FhirAppointmentDto fhirDto;
        try {
            fhirDto = objectMapper.readValue(rawPayload, FhirAppointmentDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse incoming webhook payload as FHIR Appointment. Error: {}", e.getMessage());
            OperationOutcomeDto errorOutcome = new OperationOutcomeDto(
                    "fatal", "structure", "Malformed JSON payload: " + e.getMessage()
            );
            return ResponseEntity.badRequest().body(errorOutcome);
        }

        List<OperationOutcomeDto.IssueDto> validationIssues = validator.validate(fhirDto);
        if (!validationIssues.isEmpty()) {
            log.warn("FHIR Appointment validation failed. Count of issues: {}", validationIssues.size());
            for (OperationOutcomeDto.IssueDto issue : validationIssues) {
                log.warn("Validation issue: severity={}, code={}, diagnostics={}",
                        issue.getSeverity(), issue.getCode(), issue.getDiagnostics());
            }
            OperationOutcomeDto errorOutcome = new OperationOutcomeDto();
            errorOutcome.setIssue(validationIssues);
            return ResponseEntity.badRequest().body(errorOutcome);
        }

        log.info("Received appointment webhook for patient: {}, messaging provider: {}, hospital: {}",
                fhirDto.getPatientUuid(), messagingProvider, hospitalName);

        appointmentService.processWebhook(fhirDto, messagingProvider, hospitalName, providerConfigJson);

        log.info("Appointment webhook processed and added to outbox for patient: {}", fhirDto.getPatientUuid());

        OperationOutcomeDto successOutcome = new OperationOutcomeDto(
                "information", "informational", "Appointment webhook received and added to outbox."
        );
        return ResponseEntity.ok(successOutcome);
    }
}
