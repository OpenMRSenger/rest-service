package openmrsenger.restservice.appointments.infrastructure.persistence;

import openmrsenger.restservice.appointments.domain.Appointment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentRepositoryAdapterTest {

    @Mock
    private SpringDataAppointmentRepository appointmentRepository;

    @Mock
    private SpringDataOutboxRepository outboxRepository;

    private AppointmentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AppointmentRepositoryAdapter(appointmentRepository, outboxRepository);
    }

    @Test
    void saveAppointment_ShouldMapToJpaEntity() {
        // Arrange
        UUID id = UUID.randomUUID();
        LocalDateTime date = LocalDateTime.now(ZoneOffset.UTC);
        Appointment appointment = new Appointment(id, "PAT-1", date, "Scheduled");

        // Act
        adapter.saveAppointment(appointment);

        // Assert
        ArgumentCaptor<AppointmentJpaEntity> captor = ArgumentCaptor.forClass(AppointmentJpaEntity.class);
        verify(appointmentRepository).save(captor.capture());
        
        AppointmentJpaEntity entity = captor.getValue();
        assertEquals(id, entity.getId());
        assertEquals("PAT-1", entity.getPatientReference());
        assertEquals(date, entity.getDate());
        assertEquals("Scheduled", entity.getStatus());
    }

    @Test
    void saveToOutbox_NewMessage_ShouldSave() {
        // Act
        adapter.saveToOutbox("topic", "payload");

        // Assert
        verify(outboxRepository).save(any(OutboxMessageJpaEntity.class));
    }

    @Test
    void saveToOutbox_ExistingPending_ShouldUpdateIfDifferent() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        String oldPayload = "old";
        String newPayload = "new";
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OutboxMessageJpaEntity existing = new OutboxMessageJpaEntity("topic", oldPayload, now, null, eventId);
        
        when(outboxRepository.findByEventIdAndProcessedFalseAndCancelledFalse(eventId)).thenReturn(Optional.of(existing));

        // Act
        adapter.saveToOutbox("topic", newPayload, now, null, eventId);

        // Assert
        assertEquals(newPayload, existing.getPayload());
        verify(outboxRepository).save(existing);
    }

    @Test
    void saveToOutbox_ExistingPending_ShouldNotUpdateIfSame() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        String payload = "same";
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OutboxMessageJpaEntity existing = new OutboxMessageJpaEntity("topic", payload, now, null, eventId);
        
        when(outboxRepository.findByEventIdAndProcessedFalseAndCancelledFalse(eventId)).thenReturn(Optional.of(existing));

        // Act
        adapter.saveToOutbox("topic", payload, now, null, eventId);

        // Assert
        verify(outboxRepository, never()).save(any());
    }
}
