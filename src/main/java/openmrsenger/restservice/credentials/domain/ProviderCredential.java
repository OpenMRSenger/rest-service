package openmrsenger.restservice.credentials.domain;

import java.util.UUID;

public class ProviderCredential {
    private final UUID id;
    private final String hospitalId;
    private final String providerName;
    private final String configurationJson;

    public ProviderCredential(UUID id, String hospitalId, String providerName, String configurationJson) {
        this.id = id;
        this.hospitalId = hospitalId;
        this.providerName = providerName;
        this.configurationJson = configurationJson;
    }

    public UUID getId() { return id; }
    public String getHospitalId() { return hospitalId; }
    public String getProviderName() { return providerName; }
    public String getConfigurationJson() { return configurationJson; }

}
