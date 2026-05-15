package openmrsenger.restservice.credentials.domain;

import java.util.Optional;

public interface CredentialRepository {
    Optional<ProviderCredential> findByHospitalIdAndProviderName(String hospitalId, String providerName);
}
