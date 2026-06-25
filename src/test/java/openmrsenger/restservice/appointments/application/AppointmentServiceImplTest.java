package openmrsenger.restservice.appointments.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import openmrsenger.restservice.shared.security.AesPayloadEncryptionService;
import openmrsenger.restservice.shared.security.PayloadEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static final String TEST_KEY = "hhTa0lgeWcYZ1CvUmAmAHpxbdxw4GNKD33gC8LfnswA=";

    @Mock
    private AppointmentRepository appointmentRepository;

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private PayloadEncryptionService encryptionService = new AesPayloadEncryptionService(TEST_KEY);
    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentServiceImpl(appointmentRepository, objectMapper, encryptionService);
    }

    @Test
    @DisplayName("Meldt een nieuwe afspraak aan en verifieert opslag in Outbox")
    void processWebhook_ShouldSaveToOutbox() throws Exception {
        // Arrange
        String appUuid = UUID.randomUUID().toString();
        String patientUuid = UUID.randomUUID().toString();
        String phone = "+31612345678";
        String status = "booked";

        FhirAppointmentDto dto = createValidFhirDto(appUuid, patientUuid, phone, status);

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

        String storedPayload = payloadCaptor.getValue();
        assertFalse(storedPayload.contains(provider), "De opgeslagen payload mag geen leesbare tekst bevatten (moet versleuteld zijn)");
        assertFalse(storedPayload.contains(phone), "De opgeslagen payload mag geen leesbaar telefoonnummer bevatten");

        String decrypted = encryptionService.decrypt(storedPayload);
        assertTrue(decrypted.contains(provider), "Na decryptie moet de payload de geconfigureerde provider bevatten");
    }

    private FhirAppointmentDto createValidFhirDto(String appUuid, String patientUuid, String phone, String status) {
        FhirAppointmentDto dto = new FhirAppointmentDto();
        dto.setResourceType("Appointment");
        dto.setId(appUuid);
        dto.setStatus(status);
        dto.setStart(OffsetDateTime.now(ZoneOffset.UTC).plusDays(2).toString());

        ParticipantDto participant = new ParticipantDto();
        ActorDto actor = new ActorDto();
        actor.setReference("Patient/" + patientUuid);
        actor.setDisplay("John Doe");
        
        TelecomDto telecom = new TelecomDto();
        telecom.setSystem("phone");
        telecom.setValue(phone);
        
        actor.setTelecom(List.of(telecom));
        participant.setActor(actor);
        
        dto.setParticipant(List.of(participant));
        return dto;
    }
}
