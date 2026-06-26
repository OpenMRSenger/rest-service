package openmrsenger.restservice.communications.infrastructure.persistence;

import openmrsenger.restservice.communications.application.NotificationLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Component
public class NotificationLogAdapter implements NotificationLogService {

    private static final Logger log = LoggerFactory.getLogger(NotificationLogAdapter.class);
    private final SpringDataNotificationLogRepository repository;

    public NotificationLogAdapter(SpringDataNotificationLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAlreadySent(UUID eventId) {
        return repository.findById(eventId)
                .map(notificationLog -> "SENT".equals(notificationLog.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPending(UUID eventId, String providerId, String hospitalId) {
        log.info("Logging pending notification: eventId={}, providerId={}, hospitalId={}", eventId, providerId, hospitalId);
        NotificationLogJpaEntity entity = repository.findById(eventId)
                .orElseGet(() -> new NotificationLogJpaEntity(eventId));
        
        // Only set to PENDING if not already SENT
        if (!"SENT".equals(entity.getStatus())) {
            entity.setStatus("PENDING");
            entity.setProviderId(providerId);
            entity.setHospitalId(hospitalId);
            repository.save(entity);
        }
    }

    @Override
    @Transactional
    public void logSuccess(UUID eventId) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.markAsSent();
            repository.save(entity);
        });
    }

    @Override
    @Transactional
    public void logFailure(UUID eventId, String error) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.markAsFailed(error);
            repository.save(entity);
        });
    }
}
