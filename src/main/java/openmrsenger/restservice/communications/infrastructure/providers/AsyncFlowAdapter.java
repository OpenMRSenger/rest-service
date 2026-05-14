package openmrsenger.restservice.communications.infrastructure.providers;

import openmrsenger.restservice.communications.domain.MessagingProviderPort;
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
public class AsyncFlowAdapter implements MessagingProviderPort {

    private static final Logger log = LoggerFactory.getLogger(AsyncFlowAdapter.class);
    private static final String PROVIDER_ID = "ASYNCFLOW";

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String studentGroup;

    public AsyncFlowAdapter(
            RestTemplate restTemplate,
            @Value("${ASYNC_FLOW_API_URL}") String apiUrl,
            @Value("${ASYNC_FLOW_API_KEY}") String apiKey,
            @Value("${ASYNC_FLOW_STUDENT_GROUP}") String studentGroup) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.studentGroup = studentGroup;
    }

    @Override
    public boolean supports(String providerId) {
        return PROVIDER_ID.equalsIgnoreCase(providerId);
    }

    @Override
    public void sendNotification(
            String patientId,
            String phoneNumber,
            String messageText,
            String apiKeyFromMethod) {

        log.info("Starting AsyncFlow notification for patientId={}, phone={}",
                patientId,
                phoneNumber);

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Gebruik de API key uit configuratie
            headers.set("X-API-KEY", apiKey);

            headers.set("X-STUDENT-GROUP", studentGroup);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("destination", phoneNumber);
            payload.put("content", messageText);
            payload.put("priority", "normal");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            log.debug("Sending request to AsyncFlow API at {}", apiUrl);
            log.debug("Payload: {}", payload);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            log.info(
                    "AsyncFlow message successfully sent. Status={}",
                    response.getStatusCode());

            log.debug("AsyncFlow response body={}", response.getBody());

        } catch (HttpStatusCodeException exception) {

            log.error(
                    "AsyncFlow API returned error. Status={}, Body={}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());

            throw new RuntimeException(
                    "AsyncFlow API Error: " + exception.getResponseBodyAsString(),
                    exception);

        } catch (Exception exception) {

            log.error(
                    "Unexpected AsyncFlow error for patientId={}, phone={}",
                    patientId,
                    phoneNumber,
                    exception);

            throw new RuntimeException(
                    "AsyncFlow Service Error: " + exception.getMessage(),
                    exception);
        }
    }
}