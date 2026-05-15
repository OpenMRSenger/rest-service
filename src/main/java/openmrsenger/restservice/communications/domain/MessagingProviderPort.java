package openmrsenger.restservice.communications.domain;

import openmrsenger.restservice.shared.event.NotificationRequestedEvent;

public interface MessagingProviderPort {
    boolean supports(String providerName);
    void send(NotificationRequestedEvent event, String configurationJson);
}
