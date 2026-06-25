package openmrsenger.restservice.shared.security;

/**
 * Fails application startup immediately if required secret-backed properties are missing or
 * blank. Runs during context refresh (bean instantiation), before the embedded web server
 * starts accepting traffic, so the app never serves requests with an unconfigured key/secret.
 */
public class StartupSecretsValidator {

    public StartupSecretsValidator(String encryptionKey, String webhookSecret) {
        requireConfigured("app.encryption.key", "APP_ENCRYPTION_KEY", encryptionKey);
        requireConfigured("webhook.secret", "WEBHOOK_SECRET", webhookSecret);
    }

    private static void requireConfigured(String property, String envVar, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    property + " is not configured. Set the " + envVar + " environment variable (never hardcode it in application.properties).");
        }
    }
}
