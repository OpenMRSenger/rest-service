package Openmrsenger.rest_service.application.adapters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import Openmrsenger.rest_service.application.SendMessageCommand;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.List;

@Component
public class SecurePostAdapter implements MessageAdapter {
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;
    private final String studentGroup;

    private String accessToken;
    private Instant expiryTime;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SecurePostAdapter(
            @Value("${BASE_API_URL}") String baseApiUrl,
            @Value("${SECURE_POST_CLIENT_ID}") String clientId,
            @Value("${SECURE_POST_CLIENT_SECRET}") String clientSecret,
            @Value("${STUDENT_GROUP:3}") String studentGroup) {
        this.baseUrl = baseApiUrl + "/securepost";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.studentGroup = studentGroup;
    }

    /**
     * Step 1: Obtain Access Token (with basic JSON parsing)
     */
    private void authenticate() {
        String jsonPayload = String.format(
            "{\"clientId\": \"%s\", \"clientSecret\": \"%s\"}", 
            clientId, clientSecret
        );

        try{
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/auth"))
            .header("Content-Type", "application/json")
            .header("X-STUDENT-GROUP", studentGroup)
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            

        if (response.statusCode() == 200) {
            // Manual parsing for example purposes (In production, use Jackson/Gson)
            String body = response.body();
            this.accessToken = extractValue(body, "accessToken");
            int expiresIn = Integer.parseInt(extractValue(body, "expiresIn"));
            
            // Set expiry with a 10-second safety buffer
            this.expiryTime = Instant.now().plusSeconds(expiresIn - 10);
        } else {
            throw new IllegalArgumentException("Auth failed: " + response.body());
        }
        } catch (Exception ex) {
                throw new RuntimeException("Authentication error: " + ex.getMessage());
            }
    }

    /**
     * Checks if token is still valid
     */
    private synchronized String getValidToken() throws Exception {
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            authenticate();
        }
        return accessToken;
    }

    /**
     * Step 2: Send Message (implements MessageAdapter interface)
     */
    @Override
    public ResponseEntity<String> send(SendMessageCommand command) {
        try {
            String token = getValidToken();
            List<String> recipients = command.getRecipients();
            String content = command.getContent();
            String format = command.getType();

            // Send message to each recipient
            String lastResponse = null;
            for (String recipient : recipients) {
                String jsonPayload = String.format(
                    "{\"format\": \"%s\", \"recipient\": \"%s\", \"body\": \"%s\", \"subject\": \"Message from %s\"}",
                    format, recipient, escapeJson(content), "SecurePost Adapter"
                );

                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/message"))
                    .header("Authorization", "Bearer " + token)
                    .header("X-STUDENT-GROUP", studentGroup)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() >= 400) {
                    return ResponseEntity.status(response.statusCode()).body(response.body());
                }
                lastResponse = response.body();
            }
            return ResponseEntity.ok(lastResponse);
        } catch (Exception ex) {
            return ResponseEntity.status(503).body("Error: " + ex.getMessage());
        }
    }

    @Override
    public boolean supports(String type) {
        return "SECUREPOST".equalsIgnoreCase(type);
    }

    // Helper to extract JSON values without a library
    private String extractValue(String json, String key) {
        String pattern = "\"" + key + "\":\"?([^,\"}]+)\"?";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        return m.find() ? m.group(1) : "";
    }

    // Helper to escape JSON strings
    private String escapeJson(String value) {
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
