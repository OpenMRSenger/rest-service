package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.shared.config.ProviderConfig.SwiftSendConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SwiftSendAdapter implements MessagingProviderPort {

    private static final Logger log = LoggerFactory.getLogger(SwiftSendAdapter.class);

    private static final String PROVIDER_ID = "SWIFTSEND";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiUrl;
    private final String studentGroup;

    public SwiftSendAdapter(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${SWIFT_SEND_API_URL}") String apiUrl,
            @Value("${SWIFT_SEND_STUDENT_GROUP}") String studentGroup) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiUrl = apiUrl;
        this.studentGroup = studentGroup;
    }

    @Override
    public boolean supports(String providerId) {
        return PROVIDER_ID.equalsIgnoreCase(providerId);
    }

    @Override
    public void send(NotificationRequestedEvent event, String configurationJson) {

        log.info(
                "Starting SwiftSend notification for patientId={}, phone={}",
                event.getPatientId(),
                event.getPhoneNumber());

        try {
            SwiftSendConfig config = objectMapper.readValue(configurationJson, SwiftSendConfig.class);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            headers.set("X-API-KEY", config.apiKey());
            headers.set("X-STUDENT-GROUP", studentGroup);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("Type", "SMS");
            payload.put("Recipients", new String[] { event.getPhoneNumber() });
            payload.put("Content", event.getMessageText());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            log.debug("Sending request to SwiftSend API at {}", apiUrl);
            log.debug("Payload={}", payload);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            log.info(
                    "SwiftSend message successfully sent. Status={}",
                    response.getStatusCode());

            log.debug("SwiftSend response body={}", response.getBody());

        } catch (HttpStatusCodeException exception) {

            log.error(
                    "SwiftSend API returned error. Status={}, Body={}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());

            throw new RuntimeException(
                    "SwiftSend API Error: " + exception.getResponseBodyAsString(),
                    exception);

        } catch (Exception exception) {

            log.error(
                    "Unexpected SwiftSend error for patientId={}, phone={}",
                    event.getPatientId(),
                    event.getPhoneNumber(),
                    exception);

            throw new RuntimeException(
                    "SwiftSend Service Error: " + exception.getMessage(),
                    exception);
        }
    }
}