package openmrsenger.restservice.communications.infrastructure.providers;

import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import org.springframework.stereotype.Component;

@Component
public class SecurePostAdapter implements MessagingProviderPort {

    @Override
    public boolean supports(String providerId) {
        return "SECUREPOST".equalsIgnoreCase(providerId);
    }

    @Override
    public void sendNotification(String patientId, String phoneNumber, String messageText, String apiKey) {
        System.out.println("--- SecurePostAdapter ---");
        System.out.println("Sending encrypted payload to: " + phoneNumber);
        System.out.println("Message: " + messageText);
        System.out.println("Using API Key: " + apiKey);
        // Implementation for secure post API goes here
    }
}
