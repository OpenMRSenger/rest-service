package openmrsenger.restservice.appointments.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    List<AppointmentJpaEntity> findByDateBefore(LocalDateTime cutoff);

    @Modifying
    @Query("DELETE FROM AppointmentJpaEntity a WHERE a.date < :cutoff")
    int deleteByDateBefore(@Param("cutoff") LocalDateTime cutoff);
}
