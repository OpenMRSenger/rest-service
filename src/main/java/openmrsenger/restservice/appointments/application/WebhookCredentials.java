package openmrsenger.restservice.appointments.application;

public record WebhookCredentials(
        String token,
        String username,
        String password,
        String clientId,
        String clientSecret
) {}
