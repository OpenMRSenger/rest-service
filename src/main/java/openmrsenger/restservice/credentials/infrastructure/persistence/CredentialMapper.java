package openmrsenger.restservice.credentials.infrastructure.persistence;

import openmrsenger.restservice.credentials.domain.ProviderCredential;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between ProviderCredential domain objects and JPA entities.
 */
@Component
public class CredentialMapper {

    /**
     * Converts a JPA entity to a domain object.
     */
    public ProviderCredential toDomain(ProviderCredentialJpaEntity entity) {
        if (entity == null) return null;

        return new ProviderCredential(
                entity.getId(),
                entity.getHospitalId(),
                entity.getProviderName(),
                entity.getConfigurationJson()
        );
    }

    /**
     * Converts a domain object to a JPA entity.
     */
    public ProviderCredentialJpaEntity toEntity(ProviderCredential domain) {
        if (domain == null) return null;

        return new ProviderCredentialJpaEntity(
                domain.id(),
                domain.hospitalId(),
                domain.providerName(),
                domain.configurationJson()
        );
    }
}