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

  private static final String RABBITMQ_MESSAGE_TTL = "x-message-ttl";
  private static final String RABBITMQ_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
  private static final String RABBITMQ_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

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
      .withArgument(RABBITMQ_MESSAGE_TTL, 10000)
      .withArgument(RABBITMQ_DEAD_LETTER_EXCHANGE, "")
      .withArgument(RABBITMQ_DEAD_LETTER_ROUTING_KEY, RabbitMqTopology.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsRetry60sQueue() {
    return QueueBuilder.durable(RabbitMqTopology.RETRY_QUEUE_60S)
      .withArgument(RABBITMQ_MESSAGE_TTL, 60000)
      .withArgument(RABBITMQ_DEAD_LETTER_EXCHANGE, "")
      .withArgument(RABBITMQ_DEAD_LETTER_ROUTING_KEY, RabbitMqTopology.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsRetry600sQueue() {
    return QueueBuilder.durable(RabbitMqTopology.RETRY_QUEUE_600S)
      .withArgument(RABBITMQ_MESSAGE_TTL, 600000)
      .withArgument(RABBITMQ_DEAD_LETTER_EXCHANGE, "")
      .withArgument(RABBITMQ_DEAD_LETTER_ROUTING_KEY, RabbitMqTopology.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsDlqQueue() {
    return QueueBuilder.durable(RabbitMqTopology.DLQ_QUEUE).build();
  }

}
