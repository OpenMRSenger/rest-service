package OpenMRSenger.restservice.application.adapters;

import OpenMRSenger.restservice.application.SendMessageCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Adapter for the LegacyLink SOAP-style API.
 * This class is managed as a Spring Component and handles XML-based SMS delivery.
 */
@Component
public class LegacyLinkAdapter implements MessageAdapter {

    private static final Logger log = LoggerFactory.getLogger(LegacyLinkAdapter.class);

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String username;
    private final String password;
    private final String studentGroup;

    public LegacyLinkAdapter(
            RestTemplate restTemplate,
            @Value("${legacylink.api.url}") String apiUrl,
            @Value("${legacylink.api.username}") String username,
            @Value("${legacylink.api.password}") String password,
            @Value("${legacylink.api.student-group}") String studentGroup) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        this.studentGroup = studentGroup;
    }

    /**
     * Declares support for "LEGACYLINK" message types.
     */
    @Override
    public boolean supports(String type) {
        return "LEGACYLINK".equalsIgnoreCase(type);
    }

    @Override
    public ResponseEntity<String> send(SendMessageCommand command) {
        if (command.getRecipients() == null || command.getRecipients().isEmpty()) {
            return ResponseEntity.badRequest().body("No recipients specified in the command.");
        }

        ResponseEntity<String> lastResponse = null;

        // Iterate through recipients as LegacyLink supports one number per XML request
        for (String phoneNumber : command.getRecipients()) {
            try {
                // 1. Prepare headers with Basic Auth and required custom headers
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_XML);
                headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
                headers.add("X-STUDENT-GROUP", studentGroup);
                headers.setBasicAuth(username, password);

                // 2. Build the XML payload
                String xmlPayload = buildXmlPayload(phoneNumber, command.getContent());
                HttpEntity<String> entity = new HttpEntity<>(xmlPayload, headers);

                // 3. Execute the POST request
                log.info("Sending LegacyLink SMS to {}", phoneNumber);
                lastResponse = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, String.class);

                log.info("SMS successfully sent to {}. Status: {}", phoneNumber, lastResponse.getStatusCode());

            } catch (HttpStatusCodeException ex) {
                log.error("LegacyLink API rejected request for {}: HTTP {} - {}",
                        phoneNumber, ex.getStatusCode(), ex.getResponseBodyAsString());
                return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());

            } catch (Exception ex) {
                log.error("Fatal network error during communication with LegacyLink for {}", phoneNumber, ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Internal server error: provider service unreachable.");
            }
        }
        return lastResponse;
    }

    private String buildXmlPayload(String phone, String text) {
        // XML Escaping for safety
        String safeText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");

        return """
                <?xml version="1.0" encoding="utf-8"?>
                <SendSmsRequest xmlns="http://legacylink.fakecomworld.com/v1">
                  <PhoneNumber>%s</PhoneNumber>
                  <MessageText>%s</MessageText>
                  <SenderIdentification>OpenMRSenger</SenderIdentification>
                </SendSmsRequest>
                """.formatted(phone, safeText);
    }
}