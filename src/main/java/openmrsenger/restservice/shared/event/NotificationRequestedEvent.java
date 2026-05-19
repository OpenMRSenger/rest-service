package openmrsenger.restservice.shared.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonCreator
    public NotificationRequestedEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("patientId") String patientId,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("messageText") String messageText,
            @JsonProperty("providerId") String providerId) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.occurredOn = occurredOn != null ? occurredOn : Instant.now();
        this.patientId = patientId;
        this.phoneNumber = phoneNumber;
        this.messageText = messageText;
        this.providerId = providerId;
    }

    public NotificationRequestedEvent(String patientId, String phoneNumber, String messageText, String providerId) {
        this(null, null, patientId, phoneNumber, messageText, providerId);
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
