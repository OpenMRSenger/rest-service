package openmrsenger.restservice.communications.infrastructure.providers;

import openmrsenger.restservice.shared.config.ProviderConfig.AsyncFlowConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class AsyncFlowAdapter extends AbstractRestMessagingAdapter<AsyncFlowConfig> {

    private static final String PROVIDER_ID = "ASYNCFLOW";

    public AsyncFlowAdapter(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${base.api.url}") String baseApiUrl) {
        super(restTemplate, objectMapper, baseApiUrl);
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
        headers.set("X-STUDENT-GROUP", config.studentGroup());
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
}
