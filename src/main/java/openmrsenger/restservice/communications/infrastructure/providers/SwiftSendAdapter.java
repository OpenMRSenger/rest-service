package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.communications.infrastructure.config.ProviderConfig.SwiftSendConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SwiftSendAdapter extends AbstractRestMessagingAdapter<SwiftSendConfig> {

    private static final String PROVIDER_ID = "SWIFTSEND";

    private final String studentGroup;

    public SwiftSendAdapter(
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
    protected Class<SwiftSendConfig> getConfigClass() {
        return SwiftSendConfig.class;
    }

    @Override
    protected String getEndpointPath() {
        return "/swiftsend";
    }

    @Override
    protected HttpHeaders buildHeaders(SwiftSendConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-KEY", config.apiKey());
        headers.set("X-STUDENT-GROUP", studentGroup);
        return headers;
    }

    @Override
    protected Object buildPayload(NotificationRequestedEvent event, SwiftSendConfig config) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("Type", "SMS");
        payload.put("Recipients", new String[] { event.getPhoneNumber() });
        payload.put("Content", event.getMessageText());
        return payload;
    }
}
