package openmrsenger.restservice.credentials.api;

import java.util.Optional;

public interface CredentialService {
    Optional<String> getApiKey(String hospitalId, String providerName);
}
