package openmrsenger.restservice.communications.infrastructure.providers;

import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import org.springframework.stereotype.Component;

@Component
public class AsyncFlowAdapter implements MessagingProviderPort {

    @Override
    public boolean supports(String providerId) {
        return "ASYNCFLOW".equalsIgnoreCase(providerId);
    }

    @Override
    public void sendNotification(String patientId, String phoneNumber, String messageText, String apiKey) {
        System.out.println("--- AsyncFlowAdapter ---");
        System.out.println("Queueing message for async delivery to: " + phoneNumber);
        System.out.println("Message: " + messageText);
        System.out.println("Using API Key: " + apiKey);
        // Implementation for async API goes here
    }
}
