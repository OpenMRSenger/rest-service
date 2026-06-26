package openmrsenger.restservice.communications.infrastructure.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
public class NotificationLogJpaEntity {

    @Id
    private UUID eventId;
    
    private String status; // PENDING, SENT, FAILED
    
    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;

    private String errorMessage;

    private String providerId;

    private String hospitalId;

    public NotificationLogJpaEntity() {
    }

    public NotificationLogJpaEntity(UUID eventId) {
        this.eventId = eventId;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    public UUID getEventId() {
        return eventId;
    }

    public void setEventId(UUID eventId) {
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

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public void markAsSent() {
        this.status = "SENT";
        this.processedAt = LocalDateTime.now(ZoneOffset.UTC);
        this.errorMessage = null;
    }

    public void markAsFailed(String error) {
        this.status = "FAILED";
        this.processedAt = LocalDateTime.now(ZoneOffset.UTC);
        this.errorMessage = error;
    }
}
