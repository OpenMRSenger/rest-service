package openmrsenger.restservice.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "destination", nullable = false)
    private String destination;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected OutboxMessage() {}

    public OutboxMessage(String payload, String destination) {
        this.payload = payload;
        this.destination = destination;
        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public String getPayload() { return payload; }
    public String getDestination() { return destination; }
    public LocalDateTime getCreatedAt() { return createdAt; }

}