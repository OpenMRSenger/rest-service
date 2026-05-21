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
    private final String providerId;
    private final String hospitalId;
    private final String configurationJson;

    @JsonCreator
    public NotificationRequestedEvent(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredOn") Instant occurredOn,
            @JsonProperty("patientId") String patientId,
            @JsonProperty("phoneNumber") String phoneNumber,
            @JsonProperty("messageText") String messageText,
            @JsonProperty("providerId") String providerId,
            @JsonProperty("hospitalId") String hospitalId,
            @JsonProperty("configurationJson") String configurationJson) {
        this.eventId = eventId != null ? eventId : UUID.randomUUID();
        this.occurredOn = occurredOn != null ? occurredOn : Instant.now();
        this.patientId = patientId;
        this.phoneNumber = phoneNumber;
        this.messageText = messageText;
        this.providerId = providerId;
        this.hospitalId = hospitalId;
        this.configurationJson = configurationJson;
    }

    public NotificationRequestedEvent(String patientId, String phoneNumber, String messageText,
                                      String providerId, String hospitalId, String configurationJson) {
        this(null, null, patientId, phoneNumber, messageText, providerId, hospitalId, configurationJson);
    }

    @Override public UUID getEventId() { return eventId; }
    @Override public Instant getOccurredOn() { return occurredOn; }
    public String getPatientId() { return patientId; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getMessageText() { return messageText; }
    public String getProviderId() { return providerId; }
    public String getHospitalId() { return hospitalId; }
    public String getConfigurationJson() { return configurationJson; }
}
