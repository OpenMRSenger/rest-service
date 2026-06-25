package openmrsenger.restservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import openmrsenger.restservice.shared.messaging.RabbitMqConstants;
import openmrsenger.restservice.shared.security.StartupSecretsValidator;
import org.springframework.beans.factory.annotation.Value;


import javax.net.ssl.SSLParameters;
import java.net.http.HttpClient;

@SpringBootApplication
@EnableScheduling
public class RestServiceApplication {

  private static final String RABBITMQ_MESSAGE_TTL = "x-message-ttl";
  private static final String RABBITMQ_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
  private static final String RABBITMQ_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";

  public static void main(String[] args) {
    SpringApplication.run(RestServiceApplication.class, args);
  }

  // Fails startup fast (before the web server accepts traffic) if a required secret/key
  // environment variable is missing or blank. See StartupSecretsValidator.
  @Bean
  public StartupSecretsValidator startupSecretsValidator(
          @Value("${app.encryption.key}") String encryptionKey,
          @Value("${webhook.secret}") String webhookSecret) {
    return new StartupSecretsValidator(encryptionKey, webhookSecret);
  }

  @Bean
  public RestTemplate restTemplate() {
    SSLParameters sslParameters = new SSLParameters();
    sslParameters.setProtocols(new String[]{"TLSv1.3"});
    HttpClient httpClient = HttpClient.newBuilder()
            .sslParameters(sslParameters)
            .build();
    return new RestTemplate(new JdkClientHttpRequestFactory(httpClient));
  }

  @Bean
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.findAndRegisterModules(); // Registers JavaTimeModule for LocalDateTime etc.
    return mapper;
  }

  @Bean
  public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
    return new RabbitAdmin(connectionFactory);
  }

  @Bean
  public ApplicationRunner rabbitMqInitializer(RabbitAdmin rabbitAdmin) {
    return args -> rabbitAdmin.initialize();
  }

  @Bean
  public Queue appointmentEventsQueue() {
    return QueueBuilder.durable(RabbitMqConstants.MAIN_QUEUE).build();
  }

  @Bean
  public Queue appointmentEventsRetry10sQueue() {
    return QueueBuilder.durable(RabbitMqConstants.RETRY_QUEUE_10S)
      .withArgument(RABBITMQ_MESSAGE_TTL, 10000)
      .withArgument(RABBITMQ_DEAD_LETTER_EXCHANGE, "")
      .withArgument(RABBITMQ_DEAD_LETTER_ROUTING_KEY, RabbitMqConstants.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsRetry60sQueue() {
    return QueueBuilder.durable(RabbitMqConstants.RETRY_QUEUE_60S)
      .withArgument(RABBITMQ_MESSAGE_TTL, 60000)
      .withArgument(RABBITMQ_DEAD_LETTER_EXCHANGE, "")
      .withArgument(RABBITMQ_DEAD_LETTER_ROUTING_KEY, RabbitMqConstants.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsRetry600sQueue() {
    return QueueBuilder.durable(RabbitMqConstants.RETRY_QUEUE_600S)
      .withArgument(RABBITMQ_MESSAGE_TTL, 600000)
      .withArgument(RABBITMQ_DEAD_LETTER_EXCHANGE, "")
      .withArgument(RABBITMQ_DEAD_LETTER_ROUTING_KEY, RabbitMqConstants.MAIN_QUEUE)
        .build();
  }

  @Bean
  public Queue appointmentEventsDlqQueue() {
    return QueueBuilder.durable(RabbitMqConstants.DLQ_QUEUE).build();
  }

}
