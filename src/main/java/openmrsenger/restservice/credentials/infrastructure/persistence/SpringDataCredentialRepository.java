package openmrsenger.restservice.credentials.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCredentialRepository extends JpaRepository<ProviderCredentialJpaEntity, UUID> {
    Optional<ProviderCredentialJpaEntity> findByHospitalIdAndProviderName(String hospitalId, String providerName);
}
