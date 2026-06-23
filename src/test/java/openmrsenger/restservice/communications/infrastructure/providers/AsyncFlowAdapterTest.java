package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncFlowAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AsyncFlowAdapter adapter;
    private final String baseUrl = "http://api.fake";
    private final String studentGroup = "group1";

    @BeforeEach
    void setUp() {
        adapter = new AsyncFlowAdapter(restTemplate, objectMapper, baseUrl, studentGroup);
        adapter.pollIntervalMs = 1; // Speed up tests by setting poll interval to 1ms
    }

    @Test
    void supports_AsyncFlow() {
        assertTrue(adapter.supports("ASYNCFLOW"));
        assertFalse(adapter.supports("OTHER"));
    }

    @Test
    void send_Success_Immediate() throws Exception {
        // Arrange
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(), null, "PAT-1", "+316123", "Hello", "ASYNCFLOW", "HOSP-1", null
        );
        String configJson = "{\"apiKey\":\"secret\"}";
        
        String submitResponseJson = "{\"accepted\":true,\"trackingId\":\"ASF-123\",\"message\":\"Message queued for processing\",\"submittedAt\":\"2024-01-15T10:30:00Z\"}";
        String statusResponseJson = "{\"trackingId\":\"ASF-123\",\"status\":\"Completed\",\"submittedAt\":\"2024-01-15T10:30:00Z\",\"processedAt\":\"2024-01-15T10:30:45Z\",\"errorDetails\":null}";

        // Mock POST submit
        when(restTemplate.exchange(eq(baseUrl + "/asyncflow"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(submitResponseJson));

        // Mock GET status (immediate Completed)
        when(restTemplate.exchange(eq(baseUrl + "/asyncflow/ASF-123"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(statusResponseJson));

        // Act
        adapter.send(event, configJson);

        // Assert
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(baseUrl + "/asyncflow"), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        
        HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();
        assertEquals("secret", entity.getHeaders().getFirst("X-API-KEY"));
        assertEquals(studentGroup, entity.getHeaders().getFirst("X-STUDENT-GROUP"));
        assertEquals("+316123", entity.getBody().get("destination"));
        assertEquals("Hello", entity.getBody().get("content"));

        verify(restTemplate).exchange(eq(baseUrl + "/asyncflow/ASF-123"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void send_Success_AfterPolling() throws Exception {
        // Arrange
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(), null, "PAT-1", "+316123", "Hello", "ASYNCFLOW", "HOSP-1", null
        );
        String configJson = "{\"apiKey\":\"secret\"}";

        String submitResponseJson = "{\"accepted\":true,\"trackingId\":\"ASF-123\",\"message\":\"Message queued for processing\",\"submittedAt\":\"2024-01-15T10:30:00Z\"}";
        String statusQueuedJson = "{\"trackingId\":\"ASF-123\",\"status\":\"Queued\",\"submittedAt\":\"2024-01-15T10:30:00Z\",\"processedAt\":null,\"errorDetails\":null}";
        String statusProcessingJson = "{\"trackingId\":\"ASF-123\",\"status\":\"Processing\",\"submittedAt\":\"2024-01-15T10:30:00Z\",\"processedAt\":null,\"errorDetails\":null}";
        String statusCompletedJson = "{\"trackingId\":\"ASF-123\",\"status\":\"Completed\",\"submittedAt\":\"2024-01-15T10:30:00Z\",\"processedAt\":\"2024-01-15T10:30:45Z\",\"errorDetails\":null}";

        // Mock POST submit
        when(restTemplate.exchange(eq(baseUrl + "/asyncflow"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(submitResponseJson));

        // Mock GET status transitions: Queued -> Processing -> Completed
        when(restTemplate.exchange(eq(baseUrl + "/asyncflow/ASF-123"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(statusQueuedJson))
                .thenReturn(ResponseEntity.ok(statusProcessingJson))
                .thenReturn(ResponseEntity.ok(statusCompletedJson));

        // Act
        adapter.send(event, configJson);

        // Assert
        verify(restTemplate, times(3)).exchange(eq(baseUrl + "/asyncflow/ASF-123"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));
    }

    @Test
    void send_Failure_DeliveryFailed() throws Exception {
        // Arrange
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(), null, "PAT-1", "+316123", "Hello", "ASYNCFLOW", "HOSP-1", null
        );
        String configJson = "{\"apiKey\":\"secret\"}";

        String submitResponseJson = "{\"accepted\":true,\"trackingId\":\"ASF-123\",\"message\":\"Message queued for processing\",\"submittedAt\":\"2024-01-15T10:30:00Z\"}";
        String statusFailedJson = "{\"trackingId\":\"ASF-123\",\"status\":\"Failed\",\"submittedAt\":\"2024-01-15T10:30:00Z\",\"processedAt\":null,\"errorDetails\":\"Rate limit exceeded\"}";

        // Mock POST submit
        when(restTemplate.exchange(eq(baseUrl + "/asyncflow"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(submitResponseJson));

        // Mock GET status (Immediate Failure)
        when(restTemplate.exchange(eq(baseUrl + "/asyncflow/ASF-123"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(statusFailedJson));

        // Act & Assert
        MessagingProviderException exception = assertThrows(MessagingProviderException.class, () -> {
            adapter.send(event, configJson);
        });

        assertTrue(exception.getMessage().contains("message delivery failed: Rate limit exceeded"));
    }

    @Test
    void send_Failure_Timeout() throws Exception {
        // Arrange
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(), null, "PAT-1", "+316123", "Hello", "ASYNCFLOW", "HOSP-1", null
        );
        String configJson = "{\"apiKey\":\"secret\"}";

        String submitResponseJson = "{\"accepted\":true,\"trackingId\":\"ASF-123\",\"message\":\"Message queued for processing\",\"submittedAt\":\"2024-01-15T10:30:00Z\"}";
        String statusQueuedJson = "{\"trackingId\":\"ASF-123\",\"status\":\"Queued\",\"submittedAt\":\"2024-01-15T10:30:00Z\",\"processedAt\":null,\"errorDetails\":null}";

        // Mock POST submit
        when(restTemplate.exchange(eq(baseUrl + "/asyncflow"), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(submitResponseJson));

        // Mock GET status always Queued
        when(restTemplate.exchange(eq(baseUrl + "/asyncflow/ASF-123"), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok(statusQueuedJson));

        // Act & Assert
        MessagingProviderException exception = assertThrows(MessagingProviderException.class, () -> {
            adapter.send(event, configJson);
        });

        assertTrue(exception.getMessage().contains("delivery verification timed out"));
    }
}
