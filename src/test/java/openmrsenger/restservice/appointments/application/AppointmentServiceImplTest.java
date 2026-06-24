package openmrsenger.restservice.appointments.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * UNIT TEST: AppointmentServiceImpl
 * 
 * WAAROM DIT BETROUWBAARHEID BEWIJST:
 * Deze test valideert dat de 'Outbox Pattern' correct wordt geïnitieerd.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(appointmentRepository, objectMapper);
    }

    @Test
    @DisplayName("Meldt een nieuwe afspraak aan en verifieert opslag in Outbox")
    void processWebhook_ShouldSaveToOutbox() throws Exception {
        // Arrange
        FhirAppointmentDto dto = new FhirAppointmentDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setResourceType("Appointment");
        dto.setStatus("Scheduled");
        dto.setStart(OffsetDateTime.now().plusDays(2).toString());

        TelecomDto telecom = new TelecomDto("phone", "+31612345678");
        ActorDto actor = new ActorDto("Patient/" + UUID.randomUUID().toString(), "John Doe", List.of(telecom));
        ParticipantDto participant = new ParticipantDto(actor, "accepted");
        dto.setParticipant(List.of(participant));

        String provider = "SWIFTSEND";
        String hospitalId = "HOSP-001";
        String configJson = "{\"apiKey\":\"secret\"}";

        // Act
        appointmentService.processWebhook(dto, provider, hospitalId, configJson);

        // Assert
        verify(appointmentRepository, atLeast(2)).saveToOutbox(
                eq("appointment.events"),
                any(String.class),
                any(),
                any(),
                any(UUID.class)
        );

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(appointmentRepository, atLeastOnce()).saveToOutbox(any(), payloadCaptor.capture(), any(), any(), any());
        
        assertTrue(payloadCaptor.getValue().contains(provider), "De outbox payload moet de geconfigureerde provider bevatten");
    }
}
