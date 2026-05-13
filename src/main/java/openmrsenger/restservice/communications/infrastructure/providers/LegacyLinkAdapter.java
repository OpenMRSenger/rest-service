package openmrsenger.restservice.communications.infrastructure.providers;

import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import org.springframework.stereotype.Component;

@Component
public class LegacyLinkAdapter implements MessagingProviderPort {

    @Override
    public boolean supports(String providerId) {
        return "LEGACYLINK".equalsIgnoreCase(providerId);
    }

    @Override
    public void sendNotification(String patientId, String phoneNumber, String messageText, String apiKey) {
        System.out.println("--- LegacyLinkAdapter ---");
        System.out.println("Sending message via Legacy XML API to: " + phoneNumber);
        System.out.println("Message: " + messageText);
        System.out.println("Using API Key: " + apiKey);
        // Implementation for legacy API goes here
    }
}
