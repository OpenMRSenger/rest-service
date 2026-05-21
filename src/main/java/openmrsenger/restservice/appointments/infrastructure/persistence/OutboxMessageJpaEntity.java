package openmrsenger.restservice.appointments.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "cancelled", nullable = false, columnDefinition = "boolean not null default false")
    private boolean cancelled = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now(ZoneOffset.UTC);

    @Column(name = "scheduled_for", nullable = false)
    private LocalDateTime scheduledFor = LocalDateTime.now();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    protected OutboxMessageJpaEntity() {}

    public OutboxMessageJpaEntity(String topic, String payload) {
        this(topic, payload, LocalDateTime.now(), null, null);
    }

    public OutboxMessageJpaEntity(String topic, String payload, LocalDateTime scheduledFor, LocalDateTime expiresAt) {
        this(topic, payload, scheduledFor, expiresAt, null);
    }

    public OutboxMessageJpaEntity(String topic, String payload, LocalDateTime scheduledFor, LocalDateTime expiresAt, UUID eventId) {
        this.topic = topic;
        this.payload = payload;
        this.scheduledFor = scheduledFor != null ? scheduledFor : LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.eventId = eventId;
    }

    public UUID getId() { return id; }
    public UUID getEventId() { return eventId; }
    public String getTopic() { return topic; }
    public String getPayload() { return payload; }
    public boolean isProcessed() { return processed; }
    public boolean isCancelled() { return cancelled; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
    public void setPayload(String payload) { this.payload = payload; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
