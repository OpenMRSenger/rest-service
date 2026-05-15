package openmrsenger.restservice.shared.event;

import java.time.Instant;
import java.util.UUID;

/**
 * A Fat Event containing all the data needed to send a notification.
 */
public class NotificationRequestedEvent implements DomainEvent {
    
    private final UUID eventId;
    private final Instant occurredOn;
    
    private final String patientId;
    private final String phoneNumber;
    private final String messageText;
    private final String providerId; // Identifies which messaging provider to use

    public NotificationRequestedEvent(String patientId, String phoneNumber, String messageText, String providerId) {
        this.eventId = UUID.randomUUID();
        this.occurredOn = Instant.now();
        this.patientId = patientId;
        this.phoneNumber = phoneNumber;
        this.messageText = messageText;
        this.providerId = providerId;
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public Instant getOccurredOn() {
        return occurredOn;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getMessageText() {
        return messageText;
    }

    public String getProviderId() {
        return providerId;
    }
}
