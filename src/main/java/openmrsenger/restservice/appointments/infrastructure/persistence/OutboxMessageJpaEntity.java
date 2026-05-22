package openmrsenger.restservice.appointments.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessageJpaEntity {

    private static final ZoneId AMSTERDAM_ZONE = ZoneId.of("Europe/Amsterdam");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "topic", nullable = false)
    private String topic;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "processed", nullable = false)
    private boolean processed = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now(AMSTERDAM_ZONE);

    @Column(name = "scheduled_for", nullable = false)
    private OffsetDateTime scheduledFor = OffsetDateTime.now(AMSTERDAM_ZONE);

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    protected OutboxMessageJpaEntity() {
    }

    public OutboxMessageJpaEntity(String topic, String payload) {
        this(
                topic,
                payload,
                OffsetDateTime.now(AMSTERDAM_ZONE),
                null);
    }

    public OutboxMessageJpaEntity(
            String topic,
            String payload,
            OffsetDateTime scheduledFor,
            OffsetDateTime expiresAt) {
        this.topic = topic;
        this.payload = payload;
        this.scheduledFor = scheduledFor != null
                ? scheduledFor
                : OffsetDateTime.now(AMSTERDAM_ZONE);

        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getScheduledFor() {
        return scheduledFor;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setScheduledFor(OffsetDateTime scheduledFor) {
        this.scheduledFor = scheduledFor;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}