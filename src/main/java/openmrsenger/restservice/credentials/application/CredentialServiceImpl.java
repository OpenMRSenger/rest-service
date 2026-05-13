package openmrsenger.restservice.credentials.application;

import openmrsenger.restservice.credentials.api.CredentialService;
import openmrsenger.restservice.credentials.domain.CredentialRepository;
import openmrsenger.restservice.credentials.domain.ProviderCredential;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CredentialServiceImpl implements CredentialService {

    private final CredentialRepository repository;

    public CredentialServiceImpl(CredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<String> getApiKey(String hospitalId, String providerName) {
        return repository.findByHospitalIdAndProviderName(hospitalId, providerName)
                .map(ProviderCredential::getApiKey);
    }
}
