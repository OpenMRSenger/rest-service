package openmrsenger.restservice.communications.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
public class NotificationLogJpaEntity {

    @Id
    private String eventId;
    
    private String status; // PENDING, SENT, FAILED
    
    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;
    
    private String errorMessage;

    public NotificationLogJpaEntity() {
    }

    public NotificationLogJpaEntity(String eventId) {
        this.eventId = eventId;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public void markAsSent() {
        this.status = "SENT";
        this.processedAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markAsFailed(String error) {
        this.status = "FAILED";
        this.processedAt = LocalDateTime.now();
        this.errorMessage = error;
    }
}
