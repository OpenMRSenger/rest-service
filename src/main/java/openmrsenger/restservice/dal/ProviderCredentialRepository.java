package openmrsenger.restservice.dal;

import openmrsenger.restservice.domain.ProviderCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderCredentialRepository extends JpaRepository<ProviderCredential, UUID> {

    /**
     * Haalt de specifieke credentials op voor een ziekenhuis en provider.
     * * @param hospitalId De unieke identifier van de tenant.
     * @param providerName De naam van de provider (bijv. "SWIFTSEND").
     * @return Optionele ProviderCredential met de ontsleutelde API-key.
     */
    Optional<ProviderCredential> findByHospitalIdAndProviderName(String hospitalId, String providerName);
}
