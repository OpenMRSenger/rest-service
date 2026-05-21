package openmrsenger.restservice.communications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataNotificationLogRepository extends JpaRepository<NotificationLogJpaEntity, String> {
}
