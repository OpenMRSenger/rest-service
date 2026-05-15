package openmrsenger.restservice.credentials.api;

import java.util.Optional;

public interface CredentialService {
    Optional<CredentialDto> getConfig(String hospitalId, String providerName);
}
