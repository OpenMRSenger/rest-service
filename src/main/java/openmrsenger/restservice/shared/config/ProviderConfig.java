package openmrsenger.restservice.shared.config;

public sealed interface ProviderConfig {

    record SwiftSendConfig(String apiUrl, String studentGroup, String apiKey) implements ProviderConfig {}

    record SecurePostConfig(String apiUrl, String studentGroup, String clientId, String clientSecret) implements ProviderConfig {}

    record AsyncFlowConfig(String apiUrl, String studentGroup, String apiKey) implements ProviderConfig {}

    record LegacyLinkConfig(String apiUrl, String studentGroup, String username, String password) implements ProviderConfig {}
}