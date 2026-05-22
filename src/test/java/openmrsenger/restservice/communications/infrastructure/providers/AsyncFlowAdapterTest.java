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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsyncFlowAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private AsyncFlowAdapter adapter;
    private final String baseUrl = "http://api.fake";
    private final String studentGroup = "group1";

    @BeforeEach
    void setUp() {
        adapter = new AsyncFlowAdapter(restTemplate, objectMapper, baseUrl, studentGroup);
    }

    @Test
    void supports_AsyncFlow() {
        assertTrue(adapter.supports("ASYNCFLOW"));
        assertFalse(adapter.supports("OTHER"));
    }

    @Test
    void send_Success() throws Exception {
        // Arrange
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(), null, "PAT-1", "+316123", "Hello", "ASYNCFLOW", "HOSP-1", null
        );
        String configJson = "{\"apiKey\":\"secret\"}";
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

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
    }
}
