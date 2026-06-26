package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurePostAdapterExceptionTest {

    @Mock
    private HttpClient httpClient;

    private ObjectMapper objectMapper = new ObjectMapper();
    private SecurePostAdapter adapter;
    private final String baseUrl = "http://api.fake";
    private final String studentGroup = "group1";

    @BeforeEach
    void setUp() {
        adapter = new SecurePostAdapter(objectMapper, baseUrl, studentGroup);
        ReflectionTestUtils.setField(adapter, "httpClient", httpClient);
    }

    @Test
    void send_TokenExpired_ShouldRefreshToken() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(adapter, "accessToken", "old-token");
        ReflectionTestUtils.setField(adapter, "expiryTime", Instant.now().minusSeconds(10));

        NotificationRequestedEvent event = createEvent();
        String configJson = "{\"clientId\":\"c\", \"clientSecret\":\"s\"}";

        HttpResponse<String> tokenResponse = mockResponse(200, "{\"accessToken\":\"new-token\",\"expiresIn\":3600,\"o\":\"v\"}");
        HttpResponse<String> messageResponse = mockResponse(200, "OK");

        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(tokenResponse).thenReturn(messageResponse);

        // Act
        adapter.send(event, configJson);

        // Assert
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    void send_ApiReturns500_ShouldThrowException() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(adapter, "accessToken", "token");
        ReflectionTestUtils.setField(adapter, "expiryTime", Instant.now().plusSeconds(100));

        NotificationRequestedEvent event = createEvent();
        String configJson = "{\"clientId\":\"c\", \"clientSecret\":\"s\"}";

        HttpResponse<String> errorResponse = mockResponse(500, "Internal Error");
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(errorResponse);

        // Act & Assert
        assertThrows(MessagingProviderException.class, () -> adapter.send(event, configJson));
    }

    @Test
    void send_MalformedTokenJson_ShouldThrowException() throws Exception {
        // Arrange
        NotificationRequestedEvent event = createEvent();
        String configJson = "{\"clientId\":\"c\", \"clientSecret\":\"s\"}";

        HttpResponse<String> badResponse = mockResponse(200, "{\"bad\":\"json\"}");
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(badResponse);

        // Act & Assert
        assertThrows(NumberFormatException.class, () -> adapter.send(event, configJson));
    }

    private NotificationRequestedEvent createEvent() {
        return new NotificationRequestedEvent(UUID.randomUUID(), null, "P-1", "+31", "M", "S", "H", null);
    }

    private HttpResponse<String> mockResponse(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }
}
