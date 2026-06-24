package openmrsenger.restservice.appointments.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openmrsenger.restservice.appointments.application.AppointmentService;
import openmrsenger.restservice.appointments.application.FhirAppointmentDto;
import openmrsenger.restservice.appointments.application.FhirAppointmentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@ExtendWith(MockitoExtension.class)
class AppointmentWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private WebhookAuthenticator authenticator;

    @Mock
    private FhirAppointmentValidator validator;

    @InjectMocks
    private AppointmentWebhookController controller;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void receiveAppointment_WithValidAuthAndValidPayload_ShouldReturnOkWithOperationOutcome() throws Exception {
        // Arrange
        FhirAppointmentDto dto = new FhirAppointmentDto();
        dto.setId("appointment-123");
        dto.setResourceType("Appointment");
        dto.setStatus("booked");
        dto.setStart("2026-06-24T12:00:00Z");

        when(authenticator.authenticate(any())).thenReturn(true);
        when(validator.validate(any())).thenReturn(Collections.emptyList());

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/appointments")
                .header("Authorization", "valid-token")
                .header("x-messaging-provider", "SWIFTSEND")
                .header("x-hospital-name", "HOSP-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].severity").value("information"))
                .andExpect(jsonPath("$.issue[0].code").value("informational"))
                .andExpect(jsonPath("$.issue[0].diagnostics").value("Appointment webhook received and added to outbox."));

        verify(appointmentService).processWebhook(any(FhirAppointmentDto.class), eq("SWIFTSEND"), eq("HOSP-001"), any());
    }

    @Test
    void receiveAppointment_WithValidAuthAndInvalidPayload_ShouldReturnBadRequestWithOperationOutcome() throws Exception {
        // Arrange
        FhirAppointmentDto dto = new FhirAppointmentDto();
        
        when(authenticator.authenticate(any())).thenReturn(true);
        when(validator.validate(any())).thenReturn(List.of("Missing mandatory field: resourceType", "Missing mandatory field: status"));

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/appointments")
                .header("Authorization", "valid-token")
                .header("x-messaging-provider", "SWIFTSEND")
                .header("x-hospital-name", "HOSP-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.resourceType").value("OperationOutcome"))
                .andExpect(jsonPath("$.issue[0].severity").value("error"))
                .andExpect(jsonPath("$.issue[0].code").value("invalid"))
                .andExpect(jsonPath("$.issue[0].diagnostics").value("Missing mandatory field: resourceType"))
                .andExpect(jsonPath("$.issue[1].severity").value("error"))
                .andExpect(jsonPath("$.issue[1].code").value("invalid"))
                .andExpect(jsonPath("$.issue[1].diagnostics").value("Missing mandatory field: status"));
    }

    @Test
    void receiveAppointment_WithInvalidAuth_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        FhirAppointmentDto dto = new FhirAppointmentDto();
        
        when(authenticator.authenticate(any())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/appointments")
                .header("Authorization", "invalid-token")
                .header("x-messaging-provider", "SWIFTSEND")
                .header("x-hospital-name", "HOSP-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Unauthorized: Invalid or missing bearer token."));
    }
}
