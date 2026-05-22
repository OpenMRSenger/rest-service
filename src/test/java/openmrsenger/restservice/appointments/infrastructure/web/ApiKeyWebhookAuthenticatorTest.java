package openmrsenger.restservice.appointments.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

class ApiKeyWebhookAuthenticatorTest {

    private ApiKeyWebhookAuthenticator authenticator;

    @Mock
    private HttpServletRequest request;

    private final String secretKey = "test-secret";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authenticator = new ApiKeyWebhookAuthenticator(secretKey);
    }

    @Test
    void authenticate_WithValidKey_ShouldReturnTrue() {
        when(request.getHeader("Authorization")).thenReturn(secretKey);
        assertTrue(authenticator.authenticate(request));
    }

    @Test
    void authenticate_WithInvalidKey_ShouldReturnFalse() {
        when(request.getHeader("Authorization")).thenReturn("wrong-key");
        assertFalse(authenticator.authenticate(request));
    }

    @Test
    void authenticate_WithMissingHeader_ShouldReturnFalse() {
        when(request.getHeader("Authorization")).thenReturn(null);
        assertFalse(authenticator.authenticate(request));
    }
}
