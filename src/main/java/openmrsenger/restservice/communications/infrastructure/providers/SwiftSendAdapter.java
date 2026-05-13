package openmrsenger.restservice.communications.infrastructure.providers;

import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class SwiftSendAdapter implements MessagingProviderPort {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean supports(String providerId) {
        return "SWIFTSEND".equalsIgnoreCase(providerId);
    }

    @Override
    public void sendNotification(String patientId, String phoneNumber, String messageText, String apiKey) {
        System.out.println("--- SwiftSendAdapter ---");
        System.out.println("Sending SMS to: " + phoneNumber);
        System.out.println("Message: " + messageText);
        System.out.println("Using API Key: " + apiKey);
        // External HTTP Call would go here using RestTemplate
    }
}
