package openmrsenger.restservice.communications.infrastructure.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.shared.config.ProviderConfig.SecurePostConfig;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;

@Component
public class SecurePostAdapter implements MessagingProviderPort {

    private static final Logger log = LoggerFactory.getLogger(SecurePostAdapter.class);
    private static final String PROVIDER_ID = "SECUREPOST";

    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final String studentGroup;

    private String accessToken;
    private Instant expiryTime;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public SecurePostAdapter(
            @Value("${BASE_API_URL}") String baseApiUrl,
            ObjectMapper objectMapper,
            @Value("${SECURE_POST_STUDENT_GROUP}") String studentGroup) {
        this.baseUrl = baseApiUrl + "/securepost";
        this.objectMapper = objectMapper;
        this.studentGroup = studentGroup;
    }

    @Override
    public boolean supports(String providerId) {
        return PROVIDER_ID.equalsIgnoreCase(providerId);
    }

    @Override
    public void send(NotificationRequestedEvent event, String configurationJson) {

        log.info(
                "Starting SecurePost notification for patientId={}, phone={}",
                event.getPatientId(),
                event.getPhoneNumber());

        try {
            SecurePostConfig config = objectMapper.readValue(configurationJson, SecurePostConfig.class);

            // Step 1: Get valid token
            String token = getValidToken(config);

            // Step 2: Build JSON payload
            String jsonPayload = String.format(
                    """
                            {
                                "format": "TEXT",
                                "recipient": "%s",
                                "body": "%s",
                                "subject": "Message from SecurePost"
                            }
                            """,
                    event.getPhoneNumber(),
                    escapeJson(event.getMessageText()));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/message"))
                    .header("Authorization", "Bearer " + token)
                    .header("X-STUDENT-GROUP", studentGroup)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            log.debug("Sending SecurePost request to {}", baseUrl + "/message");
            log.debug("Payload={}", jsonPayload);

            // Step 3: Execute request
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {

                log.error(
                        "SecurePost API returned error. Status={}, Body={}",
                        response.statusCode(),
                        response.body());

                throw new RuntimeException(
                        "SecurePost API Error: " + response.body());
            }

            log.info(
                    "SecurePost message successfully sent to {}. Status={}",
                    event.getPhoneNumber(),
                    response.statusCode());

            log.debug("SecurePost response body={}", response.body());

        } catch (IOException exception) {

            log.error(
                    "IO error while communicating with SecurePost for patientId={}, phone={}",
                    event.getPatientId(),
                    event.getPhoneNumber(),
                    exception);

            throw new RuntimeException(
                    "SecurePost communication error: " + exception.getMessage(),
                    exception);

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            log.error(
                    "SecurePost request interrupted for patientId={}, phone={}",
                    event.getPatientId(),
                    event.getPhoneNumber(),
                    exception);

            throw new RuntimeException(
                    "SecurePost request interrupted",
                    exception);

        } catch (Exception exception) {

            log.error(
                    "Unexpected SecurePost error for patientId={}, phone={}",
                    event.getPatientId(),
                    event.getPhoneNumber(),
                    exception);

            throw new RuntimeException(
                    "SecurePost Service Error: " + exception.getMessage(),
                    exception);
        }
    }

    /**
     * Authenticates with SecurePost API.
     */
    private synchronized void authenticate(SecurePostConfig config)
            throws IOException, InterruptedException {

        log.info("Authenticating with SecurePost API");

        String jsonPayload = String.format(
                """
                        {
                            "clientId": "%s",
                            "clientSecret": "%s"
                        }
                        """,
                config.clientId(),
                config.clientSecret());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/auth"))
                .header("Content-Type", "application/json")
                .header("X-STUDENT-GROUP", studentGroup)
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {

            log.error(
                    "SecurePost authentication failed. Status={}, Body={}",
                    response.statusCode(),
                    response.body());

            throw new IOException(
                    "Authentication failed: " + response.body());
        }

        String body = response.body();

        this.accessToken = extractValue(body, "accessToken");

        int expiresIn = Integer.parseInt(
                extractValue(body, "expiresIn"));

        // 10 second safety margin
        this.expiryTime = Instant.now()
                .plusSeconds((long) expiresIn - 10);

        log.info("SecurePost authentication successful");
    }

    /**
     * Returns valid token or refreshes it.
     */
    private synchronized String getValidToken(SecurePostConfig config)
            throws IOException, InterruptedException {

        if (accessToken == null || Instant.now().isAfter(expiryTime)) {

            log.debug("SecurePost token expired or missing. Refreshing token.");

            authenticate(config);
        }

        return accessToken;
    }

    /**
     * Extract JSON value manually.
     */
    private String extractValue(String json, String key) {

        if (json == null) {
            return null;
        }

        String pattern = "\"" + key + "\":\"?([^,\"}]+)\"?";

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(json);

        return matcher.find()
                ? matcher.group(1)
                : null;
    }

    /**
     * Escapes JSON string values safely.
     */
    private String escapeJson(String value) {

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}