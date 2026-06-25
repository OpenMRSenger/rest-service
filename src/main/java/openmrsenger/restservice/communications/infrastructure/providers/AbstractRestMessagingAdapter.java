package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.communications.infrastructure.config.ProviderConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import openmrsenger.restservice.shared.logging.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public abstract class AbstractRestMessagingAdapter<T extends ProviderConfig> implements MessagingProviderPort {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final RestTemplate restTemplate;
    protected final ObjectMapper objectMapper;
    protected final String baseApiUrl;

    protected AbstractRestMessagingAdapter(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            String baseApiUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.baseApiUrl = baseApiUrl;
    }

    protected abstract String getProviderId();
    protected abstract Class<T> getConfigClass();
    protected abstract String getEndpointPath();
    protected abstract HttpHeaders buildHeaders(T config);
    protected abstract Object buildPayload(NotificationRequestedEvent event, T config);
    protected void processResponse(ResponseEntity<String> response, T config) {
        // Hook for subclasses to post-process responses (e.g., polling for asynchronous APIs)
    }

    @Override
    public boolean supports(String providerId) {
        return getProviderId().equalsIgnoreCase(providerId);
    }

    @Override
    public void send(NotificationRequestedEvent event, String configurationJson) {

        log.info("Starting {} notification for patientId={}, phone={}",
                getProviderId(),
                event.getPatientId(),
                LogSanitizer.maskPhone(event.getPhoneNumber()));

        if (configurationJson == null || configurationJson.isBlank()) {
            throw new MessagingProviderException(getProviderId() + ": x-provider-config header is missing or empty");
        }

        try {
            T config = objectMapper.readValue(configurationJson, getConfigClass());

            HttpHeaders headers = buildHeaders(config);
            Object payload = buildPayload(event, config);

            HttpEntity<Object> request = new HttpEntity<>(payload, headers);

            String targetUrl = baseApiUrl + getEndpointPath();

            log.info("Sending {} request to {} | headers={} | payload={}",
                    getProviderId(), targetUrl, LogSanitizer.redactHeaders(headers.toSingleValueMap()), LogSanitizer.maskPayload(payload));

            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            log.info("{} response: status={} | body={}",
                    getProviderId(), response.getStatusCode(), response.getBody());

            processResponse(response, config);

        } catch (JsonProcessingException exception) {

            log.error("{} config JSON parse error: {}",
                    getProviderId(),
                    exception.getOriginalMessage() != null ? exception.getOriginalMessage() : "Invalid JSON format");

            throw new MessagingProviderException(
                    getProviderId() + " config parse error: " + (exception.getOriginalMessage() != null ? exception.getOriginalMessage() : "Invalid JSON format"),
                    exception);

        } catch (HttpStatusCodeException exception) {

            log.error("{} API returned error. Status={}, Body={}",
                    getProviderId(),
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());

            throw new MessagingProviderException(
                    getProviderId() + " API Error: " + exception.getResponseBodyAsString(),
                    exception);

        } catch (ResourceAccessException exception) {

            log.error("{} network error for patientId={}, phone={}",
                    getProviderId(),
                    event.getPatientId(),
                    LogSanitizer.maskPhone(event.getPhoneNumber()),
                    exception);

            throw new MessagingProviderException(
                    getProviderId() + " network error: " + exception.getMessage(),
                    exception);

        }
    }
}
