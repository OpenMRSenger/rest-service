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
class SwiftSendAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private SwiftSendAdapter adapter;
    private final String baseUrl = "http://api.fake";
    private final String studentGroup = "group1";

    @BeforeEach
    void setUp() {
        adapter = new SwiftSendAdapter(restTemplate, objectMapper, baseUrl, studentGroup);
    }

    @Test
    void send_Success() throws Exception {
        // Arrange
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(), null, "PAT-1", "+316123", "Hello", "SWIFTSEND", "HOSP-1", null
        );
        String configJson = "{\"apiKey\":\"secret\"}";
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        // Act
        adapter.send(event, configJson);

        // Assert
        ArgumentCaptor<HttpEntity<Map<String, Object>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(baseUrl + "/swiftsend"), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        
        HttpEntity<Map<String, Object>> entity = entityCaptor.getValue();
        assertEquals("secret", entity.getHeaders().getFirst("X-API-KEY"));
        assertEquals("Hello", entity.getBody().get("Content"));
        assertArrayEquals(new String[]{"+316123"}, (String[])entity.getBody().get("Recipients"));
    }
}
