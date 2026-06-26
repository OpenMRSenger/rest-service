package openmrsenger.restservice.shared.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * UNIT TEST: StartupSecretsValidator
 *
 * Proves the app fails fast on missing/blank secrets instead of booting insecurely.
 */
class StartupSecretsValidatorTest {

    @Test
    void constructor_AcceptsConfiguredValues() {
        assertDoesNotThrow(() -> new StartupSecretsValidator("some-key", "some-secret"));
    }

    @Test
    void constructor_RejectsNullEncryptionKey() {
        assertThrows(IllegalStateException.class, () -> new StartupSecretsValidator(null, "some-secret"));
    }

    @Test
    void constructor_RejectsBlankEncryptionKey() {
        assertThrows(IllegalStateException.class, () -> new StartupSecretsValidator("   ", "some-secret"));
    }

    @Test
    void constructor_RejectsNullWebhookSecret() {
        assertThrows(IllegalStateException.class, () -> new StartupSecretsValidator("some-key", null));
    }

    @Test
    void constructor_RejectsBlankWebhookSecret() {
        assertThrows(IllegalStateException.class, () -> new StartupSecretsValidator("some-key", ""));
    }
}
