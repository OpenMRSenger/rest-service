package openmrsenger.restservice.credentials.infrastructure.persistence;

import openmrsenger.restservice.credentials.domain.CredentialRepository;
import openmrsenger.restservice.credentials.domain.ProviderCredential;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CredentialRepositoryAdapter implements CredentialRepository {

    private final SpringDataCredentialRepository springDataRepository;
    private final CredentialMapper mapper;

    public CredentialRepositoryAdapter(SpringDataCredentialRepository springDataRepository, CredentialMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Optional<ProviderCredential> findByHospitalIdAndProviderName(String hospitalId, String providerName) {
        return springDataRepository.findByHospitalIdAndProviderName(hospitalId, providerName)
                .map(mapper::toDomain);
    }
}
