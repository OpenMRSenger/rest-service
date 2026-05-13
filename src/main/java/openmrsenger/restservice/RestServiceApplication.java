package openmrsenger.restservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.CommandLineRunner;
import openmrsenger.restservice.credentials.infrastructure.persistence.SpringDataCredentialRepository;
import openmrsenger.restservice.credentials.infrastructure.persistence.ProviderCredentialJpaEntity;

@SpringBootApplication
@EnableScheduling
public class RestServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(RestServiceApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules(); // Registers JavaTimeModule for LocalDateTime etc.
    return mapper;
  }

  @Bean
  public Queue appointmentEventsQueue() {
    return new Queue("appointment.events", true);
  }

  @Bean
  public CommandLineRunner seedDatabase(SpringDataCredentialRepository credentialRepository) {
      return args -> {
          // Check if credentials exist to avoid duplicate inserts on restarts
          if (credentialRepository.findByHospitalIdAndProviderName("DEFAULT_HOSPITAL", "SWIFTSEND").isEmpty()) {
              credentialRepository.save(new ProviderCredentialJpaEntity(null, "DEFAULT_HOSPITAL", "SWIFTSEND", "your-swiftsend-api-key-here"));
              credentialRepository.save(new ProviderCredentialJpaEntity(null, "DEFAULT_HOSPITAL", "SECUREPOST", "your-securepost-client-secret-here"));
              credentialRepository.save(new ProviderCredentialJpaEntity(null, "DEFAULT_HOSPITAL", "ASYNCFLOW", "your-asyncflow-api-key-here"));
              System.out.println("✅ Seeded test credentials for providers!");
          }
      };
  }

}
