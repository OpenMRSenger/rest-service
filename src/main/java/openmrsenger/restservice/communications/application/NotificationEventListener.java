package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.credentials.api.CredentialDto;
import openmrsenger.restservice.credentials.api.CredentialService;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationEventListener {

    private final List<MessagingProviderPort> adapters;
    private final CredentialService credentialService;
    private final ObjectMapper objectMapper;

    public NotificationEventListener(List<MessagingProviderPort> adapters, CredentialService credentialService, ObjectMapper objectMapper) {
        this.adapters = adapters;
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "appointment.events")
    public void handleNotificationEvent(String eventJson) {
        try {
            NotificationRequestedEvent event = objectMapper.readValue(eventJson, NotificationRequestedEvent.class);
            
            // 1. Fetch CredentialDto from the Dumb Vault
            CredentialDto credential = credentialService.getConfig("DEFAULT_HOSPITAL", event.getProviderId())
                    .orElseThrow(() -> new RuntimeException("Configuration not found for provider: " + event.getProviderId()));

            // 2. Find the correct provider port implementation (Strategy Pattern)
            MessagingProviderPort adapter = adapters.stream()
                    .filter(p -> p.supports(credential.providerName()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unsupported provider: " + credential.providerName()));

            // 3. Send the notification (Late Deserialization happens inside the adapter)
            adapter.send(event, credential.configurationJson());

        } catch (Exception e) {
            // Optionally: Nack or dead letter
        }
    }
}
