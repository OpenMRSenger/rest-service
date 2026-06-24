package openmrsenger.restservice.appointments.infrastructure.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import openmrsenger.restservice.appointments.application.AppointmentService;
import openmrsenger.restservice.appointments.application.OpenMrsWebhookDto;
import openmrsenger.restservice.appointments.infrastructure.web.fhir.FhirAppointmentDto;
import openmrsenger.restservice.appointments.infrastructure.web.fhir.FhirAppointmentValidator;
import openmrsenger.restservice.appointments.infrastructure.web.fhir.OperationOutcomeDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/webhooks/appointments")
public class AppointmentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentWebhookController.class);

    private final AppointmentService appointmentService;
    private final WebhookAuthenticator authenticator;
    private final ObjectMapper objectMapper;
    private final FhirAppointmentValidator validator = new FhirAppointmentValidator();

    public AppointmentWebhookController(AppointmentService appointmentService,
                                         WebhookAuthenticator authenticator,
                                         ObjectMapper objectMapper) {
        this.appointmentService = appointmentService;
        this.authenticator = authenticator;
        this.objectMapper = objectMapper;
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<?> receiveAppointment(
            @RequestBody String rawPayload,
            @RequestHeader("x-messaging-provider") String messagingProvider,
            @RequestHeader("x-hospital-name") String hospitalName,
            @RequestHeader(value = "x-provider-config", required = false) String providerConfigJson,
            HttpServletRequest request) {

        if (!authenticator.authenticate(request)) {
            log.warn("Unauthorized webhook attempt from IP: {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Unauthorized: Invalid or missing bearer token.");
        }

        FhirAppointmentDto fhirDto;
        try {
            fhirDto = objectMapper.readValue(rawPayload, FhirAppointmentDto.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse incoming webhook payload as FHIR Appointment. Error: {}", e.getMessage());
            OperationOutcomeDto errorOutcome = new OperationOutcomeDto(List.of(
                    new OperationOutcomeDto.IssueDto("fatal", "structure", "Malformed JSON payload: " + e.getMessage())
            ));
            return ResponseEntity.badRequest().body(errorOutcome);
        }

        List<OperationOutcomeDto.IssueDto> validationIssues = validator.validate(fhirDto);
        if (!validationIssues.isEmpty()) {
            log.warn("FHIR Appointment validation failed. Count of issues: {}", validationIssues.size());
            for (OperationOutcomeDto.IssueDto issue : validationIssues) {
                log.warn("Validation issue: severity={}, code={}, details={}, expression={}",
                        issue.getSeverity(), issue.getCode(), 
                        issue.getDetails() != null ? issue.getDetails().getText() : "none",
                        issue.getExpression());
            }
            return ResponseEntity.badRequest().body(new OperationOutcomeDto(validationIssues));
        }

        OpenMrsWebhookDto dto = mapToOpenMrsWebhookDto(fhirDto);

        log.info("Received appointment webhook for patient: {}, provider: {}, messaging provider: {}, hospital: {}",
                dto.getPatientUuid(), dto.getArtsName(), messagingProvider, hospitalName);

        appointmentService.processWebhook(dto, messagingProvider, hospitalName, providerConfigJson);

        log.info("Appointment webhook processed and added to outbox for patient: {}", dto.getPatientUuid());

        OperationOutcomeDto successOutcome = new OperationOutcomeDto(List.of(
                new OperationOutcomeDto.IssueDto("information", "informational", "Appointment webhook received and added to outbox.")
        ));
        return ResponseEntity.ok(successOutcome);
    }

    private OpenMrsWebhookDto mapToOpenMrsWebhookDto(FhirAppointmentDto fhirDto) {
        OpenMrsWebhookDto dto = new OpenMrsWebhookDto();
        dto.setAppointmentUuid(fhirDto.getId());
        
        String status = fhirDto.getStatus();
        if ("cancelled".equalsIgnoreCase(status)) {
            dto.setStatus("Cancelled");
        } else {
            dto.setStatus(status);
        }

        dto.setStartDateTime(OffsetDateTime.parse(fhirDto.getStart()));
        if (fhirDto.getEnd() != null && !fhirDto.getEnd().trim().isEmpty()) {
            dto.setEndDateTime(OffsetDateTime.parse(fhirDto.getEnd()));
        }
        dto.setComments(fhirDto.getDescription());

        String patientUuid = null;
        String patientName = null;
        String phoneNumber = null;
        String artsName = null;

        if (fhirDto.getParticipant() != null) {
            for (FhirAppointmentDto.ParticipantDto p : fhirDto.getParticipant()) {
                if (p.getActor() != null) {
                    String ref = p.getActor().getReference();
                    String display = p.getActor().getDisplay();
                    if (ref != null) {
                        if (ref.startsWith("Patient/")) {
                            patientUuid = ref.substring("Patient/".length());
                            if (display != null) {
                                patientName = display;
                            }
                            phoneNumber = getPhoneFromTelecom(p.getTelecom());
                            if (phoneNumber == null) {
                                phoneNumber = getPhoneFromTelecom(p.getActor().getTelecom());
                            }
                        } else if (ref.startsWith("#")) {
                            String containedId = ref.substring(1);
                            FhirAppointmentDto.ContainedResourceDto contained = findContained(fhirDto.getContained(), containedId);
                            if (contained != null && "Patient".equalsIgnoreCase(contained.getResourceType())) {
                                patientUuid = contained.getId();
                                if (display != null) {
                                    patientName = display;
                                } else if (contained.getDisplay() != null) {
                                    patientName = contained.getDisplay();
                                }
                                phoneNumber = getPhoneFromTelecom(contained.getTelecom());
                            } else if (contained != null && "Practitioner".equalsIgnoreCase(contained.getResourceType())) {
                                if (display != null) {
                                    artsName = display;
                                } else if (contained.getDisplay() != null) {
                                    artsName = contained.getDisplay();
                                }
                            }
                        } else if (ref.startsWith("Practitioner/")) {
                            if (display != null) {
                                artsName = display;
                            }
                        }
                    }
                }
            }
        }

        dto.setPatientUuid(patientUuid);
        dto.setPatientName(patientName);
        dto.setArtsName(artsName);
        dto.setPhoneNumber(phoneNumber);

        return dto;
    }

    private String getPhoneFromTelecom(List<FhirAppointmentDto.TelecomDto> telecomList) {
        if (telecomList == null) {
            return null;
        }
        for (FhirAppointmentDto.TelecomDto t : telecomList) {
            if ("phone".equalsIgnoreCase(t.getSystem()) && t.getValue() != null && !t.getValue().trim().isEmpty()) {
                return t.getValue().trim();
            }
        }
        return null;
    }

    private FhirAppointmentDto.ContainedResourceDto findContained(List<FhirAppointmentDto.ContainedResourceDto> containedList, String id) {
        if (containedList == null || id == null) {
            return null;
        }
        for (FhirAppointmentDto.ContainedResourceDto res : containedList) {
            if (id.equals(res.getId())) {
                return res;
            }
        }
        return null;
    }
}