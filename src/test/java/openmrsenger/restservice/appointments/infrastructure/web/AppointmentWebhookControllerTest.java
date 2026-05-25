package openmrsenger.restservice.appointments.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openmrsenger.restservice.appointments.application.AppointmentService;
import openmrsenger.restservice.appointments.application.OpenMrsWebhookDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentWebhookControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private WebhookAuthenticator authenticator;

    @InjectMocks
    private AppointmentWebhookController controller;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void receiveAppointment_WithValidAuth_ShouldReturnOk() throws Exception {
        // Arrange
        OpenMrsWebhookDto dto = new OpenMrsWebhookDto();
        dto.setPatientUuid("patient-123");
        
        when(authenticator.authenticate(any())).thenReturn(true);

        // Act & Assert
        mockMvc.perform(post("/api/webhooks/appointments")
                .header("Authorization", "valid-token")
                .header("x-messaging-provider", "SWIFTSEND")
                .header("x-hospital-name", "HOSP-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(content().string("Appointment webhook received and added to outbox."));

        verify(appointmentService).processWebhook(any(OpenMrsWebhookDto.class), eq("SWIFTSEND"), eq("HOSP-001"), any());
    }

    @Test
    void receiveAppointment_WithInvalidAuth_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        OpenMrsWebhookDto dto = new OpenMrsWebhookDto();
        
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
