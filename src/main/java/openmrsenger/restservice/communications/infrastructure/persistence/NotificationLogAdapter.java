package openmrsenger.restservice.communications.infrastructure.persistence;

import openmrsenger.restservice.communications.application.NotificationLogService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificationLogAdapter implements NotificationLogService {

    private final SpringDataNotificationLogRepository repository;

    public NotificationLogAdapter(SpringDataNotificationLogRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAlreadySent(String eventId) {
        return repository.findById(eventId)
                .map(log -> "SENT".equals(log.getStatus()))
                .orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logPending(String eventId) {
        NotificationLogJpaEntity entity = repository.findById(eventId)
                .orElseGet(() -> new NotificationLogJpaEntity(eventId));
        
        // Only set to PENDING if not already SENT
        if (!"SENT".equals(entity.getStatus())) {
            entity.setStatus("PENDING");
            repository.save(entity);
        }
    }

    @Override
    @Transactional
    public void logSuccess(String eventId) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.markAsSent();
            repository.save(entity);
        });
    }

    @Override
    @Transactional
    public void logFailure(String eventId, String error) {
        repository.findById(eventId).ifPresent(entity -> {
            entity.markAsFailed(error);
            repository.save(entity);
        });
    }
}
