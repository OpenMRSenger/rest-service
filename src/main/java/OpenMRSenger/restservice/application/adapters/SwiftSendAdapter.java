package OpenMRSenger.restservice.application.adapters;

import OpenMRSenger.restservice.application.SendMessageCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SwiftSendAdapter implements MessageAdapter {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String studentGroup;

    public SwiftSendAdapter(
            RestTemplate restTemplate,
            @Value("${SWIFT_SEND_API_URL}") String apiUrl,
            @Value("${SWIFT_SEND_API_KEY}") String apiKey,
            @Value("${SWIFT_SEND_STUDENT_GROUP}") String studentGroup) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.studentGroup = studentGroup;
    }

    @Override
    public ResponseEntity<String> send(SendMessageCommand command) {
        if (command.getRecipients() == null || command.getRecipients().isEmpty()) {
            return ResponseEntity.badRequest().body("No recipients specified in the command.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-KEY", apiKey);
            headers.set("X-STUDENT-GROUP", studentGroup);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("Type", command.getType());
            payload.put("Recipients", command.getRecipients());
            payload.put("Content", command.getContent());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(response.getHeaders());

            return ResponseEntity.status(response.getStatusCode()).headers(responseHeaders).body(response.getBody());
        } catch (HttpStatusCodeException exception) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(exception.getResponseHeaders() != null ? exception.getResponseHeaders() : new HttpHeaders());
            return ResponseEntity.status(exception.getStatusCode()).headers(responseHeaders).body(exception.getResponseBodyAsString());
        } catch (Exception exception) {
            return ResponseEntity.status(503).body(exception.getMessage());
        }
    }

    @Override
    public boolean supports(String type) {
        return "SWIFTSEND".equalsIgnoreCase(type);
    }
}
