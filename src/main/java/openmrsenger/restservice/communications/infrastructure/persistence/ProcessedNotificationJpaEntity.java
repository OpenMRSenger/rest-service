package openmrsenger.restservice.communications.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_notifications")
public class ProcessedNotificationJpaEntity {

    @Id
    private UUID eventId;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt = LocalDateTime.now();

    protected ProcessedNotificationJpaEntity() {}

    public ProcessedNotificationJpaEntity(UUID eventId) {
        this.eventId = eventId;
    }

    public UUID getEventId() { return eventId; }
    public LocalDateTime getProcessedAt() { return processedAt; }
}
