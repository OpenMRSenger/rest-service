package openmrsenger.restservice.appointments.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;

public interface WebhookAuthenticator {
    boolean authenticate(HttpServletRequest request);
}
