package openmrsenger.restservice.communications.infrastructure.providers;

import openmrsenger.restservice.communications.infrastructure.config.ProviderConfig.AsyncFlowConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AsyncFlowAdapter extends AbstractRestMessagingAdapter<AsyncFlowConfig> {

    private static final String PROVIDER_ID = "ASYNCFLOW";

    private final String studentGroup;

    long pollIntervalMs = 5000; // Package-private to allow test overrides

    public AsyncFlowAdapter(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${base.api.url}") String baseApiUrl,
            @Value("${student.group}") String studentGroup) {
        super(restTemplate, objectMapper, baseApiUrl);
        this.studentGroup = studentGroup;
    }

    @Override
    protected String getProviderId() {
        return PROVIDER_ID;
    }

    @Override
    protected Class<AsyncFlowConfig> getConfigClass() {
        return AsyncFlowConfig.class;
    }

    @Override
    protected String getEndpointPath() {
        return "/asyncflow";
    }

    @Override
    protected HttpHeaders buildHeaders(AsyncFlowConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", config.apiKey());
        headers.set("X-STUDENT-GROUP", studentGroup);
        return headers;
    }

    @Override
    protected Object buildPayload(NotificationRequestedEvent event, AsyncFlowConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("destination", event.getPhoneNumber());
        payload.put("content", event.getMessageText());
        payload.put("priority", "normal");
        return payload;
    }

    @Override
    protected void processResponse(ResponseEntity<String> response, AsyncFlowConfig config) {
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new MessagingProviderException(getProviderId() + " API Error: Invalid response status or body");
        }

        try {
            AsyncFlowResponse asyncResponse = objectMapper.readValue(response.getBody(), AsyncFlowResponse.class);
            String trackingId = asyncResponse.trackingId();
            if (trackingId == null || trackingId.isBlank()) {
                throw new MessagingProviderException(getProviderId() + " API error: response did not contain trackingId");
            }

            pollStatus(trackingId, config);
        } catch (JsonProcessingException e) {
            throw new MessagingProviderException(getProviderId() + " response parse error: " + e.getMessage(), e);
        }
    }

    private void pollStatus(String trackingId, AsyncFlowConfig config) {
        String statusUrl = baseApiUrl + "/asyncflow/" + trackingId;
        HttpHeaders headers = buildHeaders(config);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        int attempts = 0;
        int maxAttempts = 60; // 60 attempts * 5s = 300s (5 minutes) timeout

        while (attempts < maxAttempts) {
            try {
                log.info("Polling {} status for trackingId={}, attempt={}", getProviderId(), trackingId, attempts + 1);

                ResponseEntity<String> response = restTemplate.exchange(
                        statusUrl,
                        HttpMethod.GET,
                        requestEntity,
                        String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    AsyncFlowStatusResponse statusResponse = objectMapper.readValue(response.getBody(), AsyncFlowStatusResponse.class);
                    String status = statusResponse.status();
                    log.info("TrackingId={} status: {}", trackingId, status);

                    if ("Completed".equalsIgnoreCase(status)) {
                        log.info("Notification successfully processed for trackingId={}", trackingId);
                        return;
                    } else if ("Failed".equalsIgnoreCase(status)) {
                        String errorDetails = statusResponse.errorDetails();
                        log.error("Notification failed for trackingId={} | details: {}", trackingId, errorDetails);
                        throw new MessagingProviderException(getProviderId() + " message delivery failed: " + errorDetails);
                    }
                } else {
                    throw new MessagingProviderException(getProviderId() + " API Error status check: " + response.getBody());
                }

            } catch (HttpStatusCodeException exception) {
                log.error("Error while polling status. Status={}, Body={}", exception.getStatusCode(), exception.getResponseBodyAsString());
                throw new MessagingProviderException(getProviderId() + " API status error: " + exception.getResponseBodyAsString(), exception);
            } catch (JsonProcessingException exception) {
                log.error("JSON parse error while parsing status response: {}", exception.getMessage());
                throw new MessagingProviderException(getProviderId() + " status parse error: " + exception.getMessage(), exception);
            } catch (ResourceAccessException exception) {
                log.error("Network error while polling status: {}", exception.getMessage());
                throw new MessagingProviderException(getProviderId() + " status network error: " + exception.getMessage(), exception);
            }

            attempts++;
            try {
                Thread.sleep(pollIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MessagingProviderException(getProviderId() + " polling interrupted", e);
            }
        }

        throw new MessagingProviderException(getProviderId() + " delivery verification timed out after " + (maxAttempts * pollIntervalMs / 1000) + " seconds");
    }

    private record AsyncFlowResponse(boolean accepted, String trackingId, String message, String submittedAt) {}
    private record AsyncFlowStatusResponse(String trackingId, String status, String submittedAt, String processedAt, String errorDetails) {}
}
