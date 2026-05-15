package openmrsenger.restservice.credentials.domain;

import java.util.UUID;

public record ProviderCredential(
    UUID id,
    String hospitalId,
    String providerName,
    String configurationJson
) {}
