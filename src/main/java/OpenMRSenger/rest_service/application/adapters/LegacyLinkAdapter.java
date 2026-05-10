package OpenMRSenger.rest_service.application.adapters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

public class LegacyLinkAdapter implements MessageAdapter {

    private static final Logger log = LoggerFactory.getLogger(LegacyLinkAdapter.class);

    // Infrastructuur & Configuratie
    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String username;
    private final String password;
    private final String studentGroup;

    // Specifieke bericht data (vereist omdat send() parameterloos is)
    private final String phoneNumber;
    private final String messageText;

    /**
     * Constructor voor het aanmaken van een LegacyLink bericht.
     */
    public LegacyLinkAdapter(RestTemplate restTemplate, String apiUrl, String username,
                             String password, String studentGroup,
                             String phoneNumber, String messageText) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        this.studentGroup = studentGroup;
        this.phoneNumber = phoneNumber;
        this.messageText = messageText;
    }

    @Override
    public boolean send() {
        try {
            // 1. Headers instellen (Inclusief Basic Auth en X-STUDENT-GROUP)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);
            headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML_VALUE);
            headers.add("X-STUDENT-GROUP", studentGroup);
            headers.setBasicAuth(username, password); // Vertaalt automatisch naar Base64

            // 2. XML Payload bouwen
            String xmlPayload = buildXmlPayload(phoneNumber, messageText);
            HttpEntity<String> entity = new HttpEntity<>(xmlPayload, headers);

            // 3. Request versturen
            log.info("Verzenden van LegacyLink SMS naar {}", phoneNumber);
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            // 4. Succes controleren (Status 200 OK)
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS succesvol verzonden via LegacyLink. Response: {}", response.getBody());
                return true;
            } else {
                log.warn("Onverwachte succes-status van LegacyLink: {}", response.getStatusCode());
                return false;
            }

        } catch (HttpStatusCodeException ex) {
            // Vangt specifieke API fouten af (400, 401, 404, 500, 503)
            log.error("LegacyLink API weigerde het verzoek: HTTP {} - {}",
                    ex.getStatusCode(), ex.getResponseBodyAsString());
            return false;

        } catch (Exception ex) {
            // Vangt netwerkfouten (zoals 504 Gateway Timeout of onbereikbare server) af
            log.error("Fatale netwerkfout bij communicatie met LegacyLink", ex);
            return false;
        }
    }

    private String buildXmlPayload(String phone, String text) {
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