package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.shared.config.ProviderConfig.LegacyLinkConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LegacyLinkAdapter extends AbstractRestMessagingAdapter<LegacyLinkConfig> {

    private static final String PROVIDER_ID = "LEGACYLINK";

    public LegacyLinkAdapter(
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
    protected Class<LegacyLinkConfig> getConfigClass() {
        return LegacyLinkConfig.class;
    }

    @Override
    protected String getEndpointPath() {
        return "/legacylink/sendsms";
    }

    @Override
    protected HttpHeaders buildHeaders(LegacyLinkConfig config) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_XML_VALUE));
        headers.setBasicAuth(config.username(), config.password());
        headers.set("X-STUDENT-GROUP", config.studentGroup());
        return headers;
    }

    @Override
    protected Object buildPayload(NotificationRequestedEvent event, LegacyLinkConfig config) {
        String safeMessage = event.getMessageText()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <SendSmsRequest xmlns="http://legacylink.fakecomworld.com/v1">
                    <PhoneNumber>%s</PhoneNumber>
                    <MessageText>%s</MessageText>
                    <SenderIdentification>openmrsenger</SenderIdentification>
                </SendSmsRequest>
                """.formatted(event.getPhoneNumber(), safeMessage);
    }
}
