package openmrsenger.restservice.domain;


import jakarta.persistence.*;
import openmrsenger.restservice.infrastructure.security.AesEncryptor;

import java.util.UUID;

/**
 * Entiteit die de credentials van een externe provider opslaat per tenant.
 */
@Entity
@Table(name = "provider_credentials", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"hospital_id", "provider_name"})
})
public class ProviderCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "hospital_id", nullable = false)
    private String hospitalId;

    @Column(name = "provider_name", nullable = false)
    private String providerName;

    // De @Convert annotatie activeert de AES encryptie/decryptie automatisch bij opslaan/ophalen.
    @Convert(converter = AesEncryptor.class)
    @Column(name = "api_key", nullable = false)
    private String apiKey;

    protected ProviderCredential() {}

    public ProviderCredential(String hospitalId, String providerName, String apiKey) {
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