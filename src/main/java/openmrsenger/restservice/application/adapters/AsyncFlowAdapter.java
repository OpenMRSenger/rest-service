package openmrsenger.restservice.application.adapters;

import openmrsenger.restservice.application.SendMessageCommand;

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
public class AsyncFlowAdapter implements MessageAdapter {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;
    private final String studentGroup;

    public AsyncFlowAdapter(
            RestTemplate restTemplate,
            @Value("${ASYNC_FLOW_API_URL}") String apiUrl,
            @Value("${ASYNC_FLOW_API_KEY}") String apiKey,
            @Value("${ASYNC_FLOW_STUDENT_GROUP}") String studentGroup) {
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

        ResponseEntity<String> lastResponse = null;

        for (String destination : command.getRecipients()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("X-API-KEY", apiKey);
                headers.set("X-STUDENT-GROUP", studentGroup);

                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("destination", destination);
                payload.put("content", command.getContent());
                payload.put("priority", "normal");

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);
                lastResponse = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String.class);

            } catch (HttpStatusCodeException exception) {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.putAll(exception.getResponseHeaders() != null ? exception.getResponseHeaders() : new HttpHeaders());
                return ResponseEntity.status(exception.getStatusCode()).headers(responseHeaders).body(exception.getResponseBodyAsString());
            } catch (Exception exception) {
                return ResponseEntity.status(503).body("AsyncFlow Service Error: " + exception.getMessage());
            }
        }

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.putAll(lastResponse.getHeaders());
        return ResponseEntity.status(lastResponse.getStatusCode()).headers(responseHeaders).body(lastResponse.getBody());
    }

    @Override
    public boolean supports(String type) {
        return "ASYNCFLOW".equalsIgnoreCase(type);
    }
}
