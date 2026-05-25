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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyLinkAdapterTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper = new ObjectMapper();
    private LegacyLinkAdapter adapter;
    private final String baseUrl = "http://api.fake";
    private final String studentGroup = "group1";

    @BeforeEach
    void setUp() {
        adapter = new LegacyLinkAdapter(restTemplate, objectMapper, baseUrl, studentGroup);
    }

    @Test
    void send_Success() throws Exception {
        // Arrange
        NotificationRequestedEvent event = new NotificationRequestedEvent(
                UUID.randomUUID(), null, "PAT-1", "+316123", "Hello & Welcome", "LEGACYLINK", "HOSP-1", null
        );
        String configJson = "{\"username\":\"user\", \"password\":\"pass\"}";
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok("OK"));

        // Act
        adapter.send(event, configJson);

        // Assert
        ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(eq(baseUrl + "/legacylink/sendsms"), eq(HttpMethod.POST), entityCaptor.capture(), eq(String.class));
        
        HttpEntity<String> entity = entityCaptor.getValue();
        assertNotNull(entity.getHeaders().getFirst("Authorization"));
        assertTrue(entity.getBody().contains("<PhoneNumber>+316123</PhoneNumber>"));
        assertTrue(entity.getBody().contains("<MessageText>Hello &amp; Welcome</MessageText>"));
    }
}
