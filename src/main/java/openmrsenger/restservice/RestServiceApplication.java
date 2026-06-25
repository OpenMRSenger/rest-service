package openmrsenger.restservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import openmrsenger.restservice.shared.messaging.RabbitMqConstants;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;

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
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
      @Override
      protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
        super.prepareConnection(connection, httpMethod);
        if (connection instanceof HttpsURLConnection httpsConnection) {
          try {
            SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
            sslContext.init(null, null, null);
            httpsConnection.setSSLSocketFactory(new Tls13SocketFactory(sslContext.getSocketFactory()));
          } catch (Exception e) {
            throw new IOException("Failed to initialize SSLContext for TLSv1.3", e);
          }
        }
      }
    };
    return new RestTemplate(factory);
  }

  private static class Tls13SocketFactory extends SSLSocketFactory {
    private final SSLSocketFactory delegate;

    public Tls13SocketFactory(SSLSocketFactory delegate) {
      this.delegate = delegate;
    }

    private Socket configureSocket(Socket socket) {
      if (socket instanceof SSLSocket sslSocket) {
        sslSocket.setEnabledProtocols(new String[] {"TLSv1.3"});
      }
      return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
      return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
      return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
      return configureSocket(delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
      return configureSocket(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
      return configureSocket(delegate.createSocket(host, port, localHost, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
      return configureSocket(delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
      return configureSocket(delegate.createSocket(address, port, localAddress, localPort));
    }
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
