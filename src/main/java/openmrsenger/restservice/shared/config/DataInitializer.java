package openmrsenger.restservice.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.credentials.infrastructure.persistence.ProviderCredentialJpaEntity;
import openmrsenger.restservice.credentials.infrastructure.persistence.SpringDataCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final SpringDataCredentialRepository repository;
    private final ObjectMapper objectMapper;

    public DataInitializer(SpringDataCredentialRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (repository.count() > 0) {
            log.info("Database already seeded with credentials.");
            return;
        }

        log.info("Seeding dummy hospital credentials...");

        saveCredential("DEFAULT_HOSPITAL", "SWIFTSEND", Map.of(
                "studentGroup", "Group1",
                "apiKey", "your-api-key-here"
        ));

        saveCredential("DEFAULT_HOSPITAL", "ASYNCFLOW", Map.of(
                "studentGroup", "Group1",
                "apiKey", "asyncflow-api-key"
        ));

        saveCredential("DEFAULT_HOSPITAL", "SECUREPOST", Map.of(
                "studentGroup", "Group1",
                "clientId", "securepost-client-id",
                "clientSecret", "securepost-secret-key"
        ));

        saveCredential("DEFAULT_HOSPITAL", "LEGACYLINK", Map.of(
                "studentGroup", "Group1",
                "username", "legacylink-user",
                "password", "legacylink-password"
        ));

        saveCredential("ST_GEORGES_HOSPITAL", "SWIFTSEND", Map.of(
                "studentGroup", "Group2",
                "apiKey", "georges-swift-key"
        ));

        saveCredential("ST_GEORGES_HOSPITAL", "ASYNCFLOW", Map.of(
                "studentGroup", "Group2",
                "apiKey", "georges-async-key"
        ));

        log.info("Database seeding completed.");
    }

    private void saveCredential(String hospitalId, String providerName, Map<String, String> config) throws Exception {
        String configJson = objectMapper.writeValueAsString(config);
        ProviderCredentialJpaEntity entity = new ProviderCredentialJpaEntity(
                null, hospitalId, providerName, configJson
        );
        repository.save(entity);
    }
}
