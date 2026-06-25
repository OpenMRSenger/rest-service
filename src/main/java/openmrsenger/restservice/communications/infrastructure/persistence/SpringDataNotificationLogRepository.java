package openmrsenger.restservice.communications.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataNotificationLogRepository extends JpaRepository<NotificationLogJpaEntity, UUID> {

    List<NotificationLogJpaEntity> findByCreatedAtBefore(LocalDateTime cutoff);

    @Modifying
    @Query("UPDATE NotificationLogJpaEntity n SET n.errorMessage = NULL WHERE n.createdAt < :cutoff AND n.errorMessage IS NOT NULL")
    int anonymizeOlderThan(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM NotificationLogJpaEntity n WHERE n.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
