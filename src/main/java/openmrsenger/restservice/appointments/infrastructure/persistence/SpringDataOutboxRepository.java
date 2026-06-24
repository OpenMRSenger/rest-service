package openmrsenger.restservice.appointments.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataOutboxRepository extends JpaRepository<OutboxMessageJpaEntity, UUID> {
    List<OutboxMessageJpaEntity> findByProcessedFalseAndCancelledFalseAndScheduledForBefore(OffsetDateTime dateTime);

    Optional<OutboxMessageJpaEntity> findByEventIdAndProcessedFalseAndCancelledFalse(UUID eventId);

    boolean existsByEventIdAndProcessedTrue(UUID eventId);

    @Modifying
    @Query("UPDATE OutboxMessageJpaEntity o SET o.cancelled = true WHERE o.eventId IN :eventIds AND o.processed = false AND o.cancelled = false")
    void cancelByEventIds(@Param("eventIds") Collection<UUID> eventIds);
}
