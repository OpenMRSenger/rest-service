package openmrsenger.restservice.appointments.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyWebhookAuthenticator implements WebhookAuthenticator {

    private final String secretKey;

    public ApiKeyWebhookAuthenticator(@Value("${webhook.secret:my-secret-key}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public boolean authenticate(HttpServletRequest request) {
        String header = request.getHeader("X-Webhook-Secret");
        return secretKey.equals(header);
    }
}
