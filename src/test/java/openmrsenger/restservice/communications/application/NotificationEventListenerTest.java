package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import openmrsenger.restservice.shared.security.AesPayloadEncryptionService;
import openmrsenger.restservice.shared.security.PayloadEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock
    private MessagingProviderPort provider;

    @Mock
    private NotificationLogService logService;

    @Mock
    private EventRetryService retryService;

    private static final String TEST_KEY = "hhTa0lgeWcYZ1CvUmAmAHpxbdxw4GNKD33gC8LfnswA=";

    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private PayloadEncryptionService encryptionService = new AesPayloadEncryptionService(TEST_KEY);
    private NotificationEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new NotificationEventListener(List.of(provider), objectMapper, logService, retryService, new SimpleMeterRegistry(), encryptionService);
    }

    @Test
    void handleNotificationEvent_Success() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                eventId, Instant.now(), "PAT-001", "+31600000000", "Msg", "TEST_PROVIDER", "HOSP-1", "{}"
        );
        String encryptedPayload = encryptionService.encrypt(objectMapper.writeValueAsString(event));

        when(logService.isAlreadySent(eventId)).thenReturn(false);
        when(provider.supports("TEST_PROVIDER")).thenReturn(true);

        // Act
        listener.handleNotificationEvent(encryptedPayload, 0);

        // Assert
        verify(logService).logPending(eventId, "TEST_PROVIDER", "HOSP-1");
        verify(provider).send(any(), any());
        verify(logService).logSuccess(eventId);
        verify(retryService, never()).scheduleRetry(any(), anyInt(), any());
    }

    @Test
    void handleNotificationEvent_AlreadySent_ShouldSkip() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                eventId, Instant.now(), "PAT-001", "+31600000000", "Msg", "TEST_PROVIDER", "HOSP-1", "{}"
        );
        String encryptedPayload = encryptionService.encrypt(objectMapper.writeValueAsString(event));

        when(logService.isAlreadySent(eventId)).thenReturn(true);

        // Act
        listener.handleNotificationEvent(encryptedPayload, 0);

        // Assert
        verify(logService, never()).logPending(any(), any(), any());
        verify(provider, never()).send(any(), any());
        verify(retryService, never()).scheduleRetry(any(), anyInt(), any());
    }

    @Test
    void handleNotificationEvent_Failure_ShouldRetry() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                eventId, Instant.now(), "PAT-001", "+31600000000", "Msg", "TEST_PROVIDER", "HOSP-1", "{}"
        );
        String encryptedPayload = encryptionService.encrypt(objectMapper.writeValueAsString(event));

        when(logService.isAlreadySent(eventId)).thenReturn(false);
        when(provider.supports("TEST_PROVIDER")).thenReturn(true);
        doThrow(new IllegalStateException("Send failed")).when(provider).send(any(), any());

        // Act
        listener.handleNotificationEvent(encryptedPayload, 0);

        // Assert
        verify(logService).logFailure(eq(eventId), contains("Send failed"));
        verify(retryService).scheduleRetry(eq(encryptedPayload), eq(0), any());
    }
}
