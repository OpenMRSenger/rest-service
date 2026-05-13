package openmrsenger.restservice.credentials.infrastructure.persistence;

import jakarta.persistence.*;
import openmrsenger.restservice.shared.security.AesEncryptor;
import java.util.UUID;

@Entity
@Table(name = "provider_credentials", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hospital_id", "provider_name"})
})
public class ProviderCredentialJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    @Convert(converter = AesEncryptor.class)
    @Column(name = "api_key", nullable = false)
    private String apiKey;

    protected ProviderCredentialJpaEntity() {}

    public ProviderCredentialJpaEntity(UUID id, String hospitalId, String providerName, String apiKey) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.providerName = providerName;
        this.apiKey = apiKey;
    }

    public UUID getId() { return id; }
    public String getHospitalId() { return hospitalId; }
    public String getProviderName() { return providerName; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
}
