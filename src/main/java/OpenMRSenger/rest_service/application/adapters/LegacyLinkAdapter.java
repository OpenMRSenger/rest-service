package OpenMRSenger.rest_service.application.adapters;

import OpenMRSenger.rest_service.application.SendMessageCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class LegacyLinkAdapter implements MessageAdapter {

    private static final Logger log = LoggerFactory.getLogger(LegacyLinkAdapter.class);

    private final RestTemplate restTemplate;

    private String apiUrl;
    private String username;
    private String password;
    private String studentGroup;

    public LegacyLinkAdapter(RestTemplate restTemplate, String apiUrl, String username, String password, String studentGroup) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        this.studentGroup = studentGroup;
    }

    @Override
    public ResponseEntity<String> send(SendMessageCommand command) {
        if (command.recipients() == null || command.recipients().isEmpty()) {
            return ResponseEntity.badRequest().body("No recipients specified in the command.");
        }

        ResponseEntity<String> response = null;

        // LegacyLink supports 1 number per XML request, so we iterate over the list
        for (String phoneNumber : command.recipients()) {
            try {
                // 1. Build headers (including automatic Base64 Basic Auth)
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_XML);
                headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
                headers.add("X-STUDENT-GROUP", studentGroup);
                headers.setBasicAuth(username, password);

                // 2. Build XML payload with data from the command
                String xmlPayload = buildXmlPayload(phoneNumber, command.content());
                HttpEntity<String> entity = new HttpEntity<>(xmlPayload, headers);

                // 3. Execute request
                log.info("Sending LegacyLink SMS to {}", phoneNumber);
                response = restTemplate.exchange(
                        apiUrl,
                        HttpMethod.POST,
                        entity,
                        String.class
                );

                log.info("SMS successfully sent to {}. Status: {}", phoneNumber, response.getStatusCode());

            } catch (HttpStatusCodeException ex) {
                log.error("LegacyLink API rejected request for {}: HTTP {} - {}",
                        phoneNumber, ex.getStatusCode(), ex.getResponseBodyAsString());

                return ResponseEntity.status(ex.getStatusCode()).body(ex.getResponseBodyAsString());

            } catch (Exception ex) {
                log.error("Fatal network error during communication with LegacyLink for {}", phoneNumber, ex);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Internal server error: service unreachable.");
            }
        }
        return response;
    }

    private String buildXmlPayload(String phone, String text) {
        // Basic escaping to prevent invalid XML
        String safeText = text.replace("<", "&lt;").replace(">", "&gt;").replace("&", "&amp;");

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