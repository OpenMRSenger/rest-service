package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.communications.infrastructure.config.ProviderConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
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

    @Override
    public boolean supports(String providerId) {
        return getProviderId().equalsIgnoreCase(providerId);
    }

    @Override
    public void send(NotificationRequestedEvent event, String configurationJson) {

        log.info("Starting {} notification for patientId={}, phone={}",
                getProviderId(),
                event.getPatientId(),
                event.getPhoneNumber());

        try {
            T config = objectMapper.readValue(configurationJson, getConfigClass());

            HttpHeaders headers = buildHeaders(config);
            Object payload = buildPayload(event, config);

            HttpEntity<Object> request = new HttpEntity<>(payload, headers);

            String targetUrl = baseApiUrl + getEndpointPath();

            log.debug("Sending request to {} API at {}", getProviderId(), targetUrl);
            log.debug("Payload: {}", payload);

            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            log.info("{} message successfully sent. Status={}",
                    getProviderId(),
                    response.getStatusCode());

            log.debug("{} response body={}", getProviderId(), response.getBody());

        } catch (JsonProcessingException exception) {

            log.error("{} config JSON parse error: {}",
                    getProviderId(),
                    exception.getMessage(),
                    exception);

            throw new MessagingProviderException(
                    getProviderId() + " config parse error: " + exception.getMessage(),
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
                    event.getPhoneNumber(),
                    exception);

            throw new MessagingProviderException(
                    getProviderId() + " network error: " + exception.getMessage(),
                    exception);

        }
    }
}
