package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.infrastructure.messaging.RabbitMqTopology;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.credentials.api.CredentialService;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final List<MessagingProviderPort> providers;
    private final CredentialService credentialService;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    public NotificationEventListener(
            List<MessagingProviderPort> providers,
            CredentialService credentialService,
            ObjectMapper objectMapper,
            RabbitTemplate rabbitTemplate) {
        this.providers = providers;
        this.credentialService = credentialService;
        this.objectMapper = objectMapper;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMqTopology.MAIN_QUEUE)
    public void handleNotificationEvent(Message message) {
        String eventJson = new String(message.getBody(), StandardCharsets.UTF_8);
        try {
            NotificationRequestedEvent event = objectMapper.readValue(eventJson, NotificationRequestedEvent.class);
            
            // 1. Fetch API Key via Cross-Module Call
            String apiKey = credentialService.getApiKey("DEFAULT_HOSPITAL", event.getProviderId())
                    .orElseThrow(() -> new RuntimeException("API Key not found for provider: " + event.getProviderId()));

            // 2. Find the correct provider port implementation
            MessagingProviderPort provider = providers.stream()
                    .filter(p -> p.supports(event.getProviderId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unsupported provider: " + event.getProviderId()));

            // 3. Send the notification
            provider.sendNotification(event.getPatientId(), event.getPhoneNumber(), event.getMessageText(), apiKey);

        } catch (Exception e) {
            int retryStage = getRetryStage(message);
            String targetQueue = switch (retryStage) {
                case 0 -> RabbitMqTopology.RETRY_QUEUE_10S;
                case 1 -> RabbitMqTopology.RETRY_QUEUE_60S;
                case 2 -> RabbitMqTopology.RETRY_QUEUE_600S;
                default -> RabbitMqTopology.DLQ_QUEUE;
            };

            if (RabbitMqTopology.DLQ_QUEUE.equals(targetQueue)) {
                log.error("Sending message to DLQ after {} retries: {}", retryStage, e.getMessage());
            } else {
                log.warn(
                        "Retry stage {} failed, moving message to {}: {}",
                        retryStage,
                        targetQueue,
                        e.getMessage());
            }

            publishWithRetryStage(eventJson, message, targetQueue, Math.min(retryStage + 1, 3));
        }
    }

    private int getRetryStage(Message message) {
        Object retryStage = message.getMessageProperties().getHeaders().get(RabbitMqTopology.RETRY_STAGE_HEADER);
        if (retryStage instanceof Number number) {
            return number.intValue();
        }
        if (retryStage instanceof String retryStageValue) {
            try {
                return Integer.parseInt(retryStageValue);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private void publishWithRetryStage(String eventJson, Message sourceMessage, String targetQueue, int nextRetryStage) {
        MessagePostProcessor messagePostProcessor = targetMessage -> {
            MessageProperties messageProperties = targetMessage.getMessageProperties();
            messageProperties.getHeaders().putAll(sourceMessage.getMessageProperties().getHeaders());
            messageProperties.setHeader(RabbitMqTopology.RETRY_STAGE_HEADER, nextRetryStage);
            messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return targetMessage;
        };

        rabbitTemplate.convertAndSend("", targetQueue, eventJson, messagePostProcessor);
    }
}
