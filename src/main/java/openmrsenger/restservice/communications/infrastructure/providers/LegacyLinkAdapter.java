package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.shared.config.ProviderConfig.LegacyLinkConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
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
        private final ObjectMapper objectMapper;
        private final String apiUrl;
        private final String studentGroup;

        public LegacyLinkAdapter(
                        RestTemplate restTemplate,
                        ObjectMapper objectMapper,
                        @Value("${LEGACYLINK_API_URL}") String apiUrl,
                        @Value("${LEGACYLINK_STUDENT_GROUP}") String studentGroup) {
                this.restTemplate = restTemplate;
                this.objectMapper = objectMapper;
                this.apiUrl = apiUrl;
                this.studentGroup = studentGroup;
        }

        @Override
        public boolean supports(String providerId) {
                return PROVIDER_ID.equalsIgnoreCase(providerId);
        }

        @Override
        public void send(NotificationRequestedEvent event, String configurationJson) {

                log.info(
                                "Starting LegacyLink notification for patientId={}, phone={}",
                                event.getPatientId(),
                                event.getPhoneNumber());

                try {
                        LegacyLinkConfig config = objectMapper.readValue(configurationJson, LegacyLinkConfig.class);

                        // 1. HTTP Headers
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_XML);
                        headers.setAccept(MediaType.parseMediaTypes(MediaType.APPLICATION_XML_VALUE));

                        // Basic Authentication using deserialized config
                        headers.setBasicAuth(config.username(), config.password());

                        // Custom Header
                        headers.set("X-STUDENT-GROUP", studentGroup);

                        // 2. XML Payload
                        String xmlPayload = buildXmlPayload(event.getPhoneNumber(), event.getMessageText());

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
                                        event.getPatientId(),
                                        event.getPhoneNumber(),
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
