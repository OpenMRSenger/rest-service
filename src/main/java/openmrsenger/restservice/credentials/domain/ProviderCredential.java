package openmrsenger.restservice.credentials.domain;

import java.util.UUID;

public class ProviderCredential {
    private final UUID id;
    private final String hospitalId;
    private final String providerName;
    private String apiKey;

    public ProviderCredential(UUID id, String hospitalId, String providerName, String apiKey) {
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
