package openmrsenger.restservice.appointments.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyWebhookAuthenticator implements WebhookAuthenticator {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyWebhookAuthenticator.class);
    private final String secretKey;

    public ApiKeyWebhookAuthenticator(@Value("${webhook.secret:my-secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public boolean authenticate(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        log.info("Received webhook with Authorization header: {}", authHeader != null ? "[PRESENT]" : "[MISSING]");
        if (authHeader == null) {
            log.warn("Webhook received without Authorization header");
            return false;
        }
        return secretKey.equals(authHeader);
    }
}