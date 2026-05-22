package openmrsenger.restservice.communications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "processed_notifications")
public class ProcessedNotificationJpaEntity {

    private static final ZoneId AMSTERDAM_ZONE = ZoneId.of("Europe/Amsterdam");

    @Id
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt = OffsetDateTime.now(AMSTERDAM_ZONE);

    protected ProcessedNotificationJpaEntity() {}

    public ProcessedNotificationJpaEntity(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() { return eventId; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
}
