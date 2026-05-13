package openmrsenger.restservice.credentials.infrastructure.persistence;

import openmrsenger.restservice.credentials.domain.ProviderCredential;
import org.springframework.stereotype.Component;

@Component
public class CredentialMapper {

    public ProviderCredential toDomain(ProviderCredentialJpaEntity entity) {
        if (entity == null) return null;
        return new ProviderCredential(
                entity.getId(),
                entity.getHospitalId(),
                entity.getProviderName(),
                entity.getApiKey()
        );
    }

    public ProviderCredentialJpaEntity toEntity(ProviderCredential domain) {
        if (domain == null) return null;
        return new ProviderCredentialJpaEntity(
                domain.getId(),
                domain.getHospitalId(),
                domain.getProviderName(),
                domain.getApiKey()
        );
    }
}
