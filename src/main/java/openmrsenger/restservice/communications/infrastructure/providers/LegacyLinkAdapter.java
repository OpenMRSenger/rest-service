package openmrsenger.restservice.communications.infrastructure.providers;

import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
public class LegacyLinkAdapter implements MessagingProviderPort {

    private static final Logger log = LoggerFactory.getLogger(LegacyLinkAdapter.class);
    private static final String PROVIDER_ID = "LEGACYLINK";

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String username;
    private final String password;
    private final String studentGroup;

    public LegacyLinkAdapter(
            RestTemplate restTemplate,
            @Value("${LEGACYLINK_API_URL}") String apiUrl,
            @Value("${LEGACYLINK_API_USERNAME}") String username,
            @Value("${LEGACYLINK_API_PASSWORD}") String password,
            @Value("${LEGACYLINK_STUDENT_GROUP}") String studentGroup) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
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
            String apiKey) {

        log.info(
                "Starting LegacyLink notification for patientId={}, phone={}",
                patientId,
                phoneNumber);

        try {

            // 1. HTTP Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_XML_VALUE));

            // Basic Authentication
            headers.setBasicAuth(username, password);

            // Custom Header
            headers.set("X-STUDENT-GROUP", studentGroup);

            // 2. XML Payload
            String xmlPayload = buildXmlPayload(phoneNumber, messageText);

            HttpEntity<String> request = new HttpEntity<>(xmlPayload, headers);

            log.debug("Sending XML request to LegacyLink API at {}", apiUrl);
            log.debug("XML Payload: {}", xmlPayload);

            // 3. Execute Request
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    request,
                    String.class);

            log.info(
                    "LegacyLink message successfully sent. Status={}",
                    response.getStatusCode());

            log.debug("LegacyLink response body={}", response.getBody());

        } catch (HttpStatusCodeException exception) {

            log.error(
                    "LegacyLink API error. Status={}, Body={}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString());

            throw new RuntimeException(
                    "LegacyLink API Error: " + exception.getResponseBodyAsString(),
                    exception);

        } catch (Exception exception) {

            log.error(
                    "Unexpected LegacyLink error for patientId={}, phone={}",
                    patientId,
                    phoneNumber,
                    exception);

            throw new RuntimeException(
                    "LegacyLink Service Error: " + exception.getMessage(),
                    exception);
        }
    }

    /**
     * Builds XML payload for LegacyLink SOAP/XML API.
     */
    private String buildXmlPayload(String phoneNumber, String messageText) {

        // Basic XML escaping
        String safeMessage = messageText
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
                """.formatted(phoneNumber, safeMessage);
    }
}