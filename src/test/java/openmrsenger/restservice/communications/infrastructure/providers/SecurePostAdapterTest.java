package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurePostAdapterTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private ObjectMapper objectMapper = new ObjectMapper();
    private SecurePostAdapter adapter;
    private final String baseUrl = "http://api.fake";
    private final String studentGroup = "group1";

    @BeforeEach
    void setUp() {
        adapter = new SecurePostAdapter(objectMapper, baseUrl, studentGroup);
        // Inject mocked HttpClient
        ReflectionTestUtils.setField(adapter, "httpClient", httpClient);
    }

    @Test
    void send_SuccessWithTokenFlow() throws Exception {
        // Arrange
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(), null, "PAT-1", "+316123", "Hello", "SECUREPOST", "HOSP-1", null
        );
        String configJson = "{\"clientId\":\"client\", \"clientSecret\":\"secret\"}";
        
        // Mock Token response
        HttpResponse<String> tokenResponse = mock(HttpResponse.class);
        when(tokenResponse.statusCode()).thenReturn(200);
        when(tokenResponse.body()).thenReturn("{\"accessToken\":\"fake-token\",\"expiresIn\":3600,\"other\":\"value\"}");
        
        // Mock Message response
        HttpResponse<String> messageResponse = mock(HttpResponse.class);
        when(messageResponse.statusCode()).thenReturn(200);
        when(messageResponse.body()).thenReturn("OK");

        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(tokenResponse)
                .thenReturn(messageResponse);

        // Act
        adapter.send(event, configJson);

        // Assert
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requestCaptor.capture(), any());
        
        HttpRequest messageReq = requestCaptor.getAllValues().get(1);
        assertEquals("Bearer fake-token", messageReq.headers().firstValue("Authorization").orElse(null));
        assertEquals(studentGroup, messageReq.headers().firstValue("X-STUDENT-GROUP").orElse(null));
    }
    
    private void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
