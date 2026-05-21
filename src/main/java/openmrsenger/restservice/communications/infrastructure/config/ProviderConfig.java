package openmrsenger.restservice.communications.infrastructure.config;

public interface ProviderConfig {

    record SwiftSendConfig(String apiKey) implements ProviderConfig {}

    record SecurePostConfig(String clientId, String clientSecret) implements ProviderConfig {}

    record AsyncFlowConfig(String apiKey) implements ProviderConfig {}

    record LegacyLinkConfig(String username, String password) implements ProviderConfig {}
}