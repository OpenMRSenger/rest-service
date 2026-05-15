package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.shared.config.ProviderConfig.AsyncFlowConfig;
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
public class AsyncFlowAdapter implements MessagingProviderPort {

        private static final Logger log = LoggerFactory.getLogger(AsyncFlowAdapter.class);
        private static final String PROVIDER_ID = "ASYNCFLOW";

        private final RestTemplate restTemplate;
        private final ObjectMapper objectMapper;
        private final String baseApiUrl;

        public AsyncFlowAdapter(
                        RestTemplate restTemplate,
                        ObjectMapper objectMapper,
                        @Value("${base.api.url}") String baseApiUrl) {
                this.restTemplate = restTemplate;
                this.objectMapper = objectMapper;
                this.baseApiUrl = baseApiUrl;
        }

        @Override
        public boolean supports(String providerId) {
                return PROVIDER_ID.equalsIgnoreCase(providerId);
        }

        @Override
        public void send(NotificationRequestedEvent event, String configurationJson) {

                log.info("Starting AsyncFlow notification for patientId={}, phone={}",
                                event.getPatientId(),
                                event.getPhoneNumber());

                try {
                        AsyncFlowConfig config = objectMapper.readValue(configurationJson, AsyncFlowConfig.class);

                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);

                        // Use the API key from the deserialized configuration
                        headers.set("X-API-KEY", config.apiKey());

                        headers.set("X-STUDENT-GROUP", config.studentGroup());

                        Map<String, Object> payload = new LinkedHashMap<>();
                        payload.put("destination", event.getPhoneNumber());
                        payload.put("content", event.getMessageText());
                        payload.put("priority", "normal");

                        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                        String targetUrl = baseApiUrl + "/asyncflow";

                        log.debug("Sending request to AsyncFlow API at {}", targetUrl);
                        log.debug("Payload: {}", payload);

                        ResponseEntity<String> response = restTemplate.exchange(
                                        targetUrl,
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
                                        event.getPatientId(),
                                        event.getPhoneNumber(),
                                        exception);

                        throw new RuntimeException(
                                        "AsyncFlow Service Error: " + exception.getMessage(),
                                        exception);
                }
        }
}
