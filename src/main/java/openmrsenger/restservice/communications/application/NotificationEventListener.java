package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.credentials.api.CredentialService;
import openmrsenger.restservice.credentials.api.CredentialDto;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;


import java.util.List;

@Service
public class NotificationEventListener {

    private final List<MessagingProviderPort> providers;
    private final CredentialService credentialService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(List<MessagingProviderPort> providers, CredentialService credentialService, ObjectMapper objectMapper) {
        this.providers = providers;
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "appointment.events")
    public void handleNotificationEvent(String eventJson) {
        try {
            NotificationRequestedEvent event = objectMapper.readValue(eventJson, NotificationRequestedEvent.class);
            
            // 1. Fetch API Key via Cross-Module Call
            CredentialDto credential = credentialService.getConfig("DEFAULT_HOSPITAL", event.getProviderId())
                    .orElseThrow(() -> new RuntimeException("API Key not found for provider: " + event.getProviderId()));

            // 2. Find the correct provider port implementation
            MessagingProviderPort provider = providers.stream()
                    .filter(p -> p.supports(event.getProviderId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unsupported provider: " + event.getProviderId()));

            // 3. Send the notification
            provider.send(event, credential.configurationJson());

        } catch (Exception e) {
            // Optionally: Nack or dead letter
        }
    }
}
    