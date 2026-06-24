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
import java.time.ZoneOffset;
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

    // Fix: Registreer JavaTimeModule voor Instant/OffsetDateTime ondersteuning
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
        OpenMrsWebhookDto dto = new OpenMrsWebhookDto();
        dto.setAppointmentUuid(UUID.randomUUID().toString());
        dto.setPatientUuid(UUID.randomUUID().toString());
        dto.setPhoneNumber("+31612345678");
        dto.setStatus("Scheduled");
        // Fix: Zet tijd ruim in de toekomst (2 dagen) om "past reminder" logs te voorkomen
        dto.setStartDateTime(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2));

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
