package openmrsenger.restservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.CommandLineRunner;
import openmrsenger.restservice.appointments.infrastructure.messaging.RabbitMqTopology;
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
    return QueueBuilder.durable(RabbitMqTopology.MAIN_QUEUE).build();
  }

  @Bean
  public Queue appointmentEventsRetry10sQueue() {
    return QueueBuilder.durable(RabbitMqTopology.RETRY_QUEUE_10S)
        .withArgument("x-message-ttl", 10000)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", RabbitMqTopology.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsRetry60sQueue() {
    return QueueBuilder.durable(RabbitMqTopology.RETRY_QUEUE_60S)
        .withArgument("x-message-ttl", 60000)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", RabbitMqTopology.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsRetry600sQueue() {
    return QueueBuilder.durable(RabbitMqTopology.RETRY_QUEUE_600S)
        .withArgument("x-message-ttl", 600000)
        .withArgument("x-dead-letter-exchange", "")
        .withArgument("x-dead-letter-routing-key", RabbitMqTopology.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsDlqQueue() {
    return QueueBuilder.durable(RabbitMqTopology.DLQ_QUEUE).build();
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
