package openmrsenger.restservice.shared.config;

public sealed interface ProviderConfig {

    record SwiftSendConfig(String studentGroup, String apiKey) implements ProviderConfig {}

    record SecurePostConfig(String studentGroup, String clientId, String clientSecret) implements ProviderConfig {}

    record AsyncFlowConfig(String studentGroup, String apiKey) implements ProviderConfig {}

    record LegacyLinkConfig(String studentGroup, String username, String password) implements ProviderConfig {}
}