package openmrsenger.restservice.communications.infrastructure.messaging;

import openmrsenger.restservice.shared.messaging.RabbitMqConstants;
import openmrsenger.restservice.communications.application.EventRetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class RabbitMqEventRetryService implements EventRetryService {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqEventRetryService.class);
    
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqEventRetryService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void scheduleRetry(String eventJson, int currentRetryStage, Exception cause) {
        int nextStage = currentRetryStage + 1;
        String routingKey = determineRoutingKey(nextStage);

        log.info("Scheduling retry for stage {} (next: {}) using routing key: {}", currentRetryStage, nextStage, routingKey);

        rabbitTemplate.convertAndSend(routingKey, (Object) eventJson, message -> {
            MessageProperties props = message.getMessageProperties();
            props.setHeader(RabbitMqConstants.RETRY_STAGE_HEADER, nextStage);
            return message;
        });
    }

    private String determineRoutingKey(int stage) {
        return switch (stage) {
            case 1 -> RabbitMqConstants.RETRY_QUEUE_10S;
            case 2 -> RabbitMqConstants.RETRY_QUEUE_60S;
            case 3 -> RabbitMqConstants.RETRY_QUEUE_600S;
            default -> {
                log.warn("Max retry stages reached. Sending to DLQ.");
                yield RabbitMqConstants.DLQ_QUEUE;
            }
        };
    }
}
