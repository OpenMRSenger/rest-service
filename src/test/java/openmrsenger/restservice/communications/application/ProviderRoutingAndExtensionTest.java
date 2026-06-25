package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import openmrsenger.restservice.shared.security.AesPayloadEncryptionService;
import openmrsenger.restservice.shared.security.PayloadEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

/**
 * UNIT TEST: Provider Routing & Extensibility
 */
@ExtendWith(MockitoExtension.class)
class ProviderRoutingAndExtensionTest {

    @Mock
    private MessagingProviderPort swiftSendAdapter;

    @Mock
    private MessagingProviderPort securePostAdapter;

    @Mock
    private NotificationLogService notificationLogService;

    @Mock
    private EventRetryService eventRetryService;

    private static final String TEST_KEY = "hhTa0lgeWcYZ1CvUmAmAHpxbdxw4GNKD33gC8LfnswA=";

    private NotificationEventListener eventListener;

    // Fix: Registreer JavaTimeModule voor Instant ondersteuning
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private PayloadEncryptionService encryptionService = new AesPayloadEncryptionService(TEST_KEY);

    @BeforeEach
    void setUp() {
        lenient().when(swiftSendAdapter.supports("SWIFTSEND")).thenReturn(true);
        lenient().when(securePostAdapter.supports("SECUREPOST")).thenReturn(true);

        eventListener = new NotificationEventListener(
                List.of(swiftSendAdapter, securePostAdapter),
                objectMapper,
                notificationLogService,
                eventRetryService,
                new SimpleMeterRegistry(),
                encryptionService
        );
    }

    @Test
    @DisplayName("Routeert correct naar SwiftSendAdapter bij configuratie")
    void handleEvent_ShouldRouteToSwiftSend() throws Exception {
        NotificationRequestedEvent event = createEvent("SWIFTSEND");
        String encryptedPayload = encryptionService.encrypt(objectMapper.writeValueAsString(event));

        eventListener.handleNotificationEvent(encryptedPayload, 0);

        verify(swiftSendAdapter, times(1)).send(any(), any());
        verify(securePostAdapter, never()).send(any(), any());
    }

    @Test
    @DisplayName("Routeert naar SecurePostAdapter en valideert thread-safe token handling potentie")
    void handleEvent_ShouldRouteToSecurePost() throws Exception {
        NotificationRequestedEvent event = createEvent("SECUREPOST");
        String encryptedPayload = encryptionService.encrypt(objectMapper.writeValueAsString(event));

        eventListener.handleNotificationEvent(encryptedPayload, 0);

        verify(securePostAdapter, times(1)).send(any(), any());
        verify(swiftSendAdapter, never()).send(any(), any());
    }

    private NotificationRequestedEvent createEvent(String provider) {
        return new NotificationRequestedEvent(
                UUID.randomUUID(),
                Instant.now(),
                "PAT-001",
                "+31600000000",
                "Test Message",
                provider,
                "HOSP-1",
                "{}"
        );
    }
}
