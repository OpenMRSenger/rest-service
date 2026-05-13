package openmrsenger.restservice.appointments.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataOutboxRepository extends JpaRepository<OutboxMessageJpaEntity, UUID> {
    List<OutboxMessageJpaEntity> findByProcessedFalse();
}
