package openmrsenger.restservice.appointments.infrastructure.messaging;

import openmrsenger.restservice.appointments.infrastructure.persistence.OutboxMessageJpaEntity;
import openmrsenger.restservice.appointments.infrastructure.persistence.SpringDataOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RabbitMqOutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(RabbitMqOutboxRelay.class);
    private final SpringDataOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqOutboxRelay(SpringDataOutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<OutboxMessageJpaEntity> pendingMessages = outboxRepository.findByProcessedFalse();
        
        if (!pendingMessages.isEmpty()) {
            log.info("Found {} pending messages in outbox", pendingMessages.size());
        }

        for (OutboxMessageJpaEntity message : pendingMessages) {
            try {
                log.info("Relaying message {} to topic {}", message.getId(), message.getTopic());
                rabbitTemplate.convertAndSend("", message.getTopic(), message.getPayload());
                message.setProcessed(true);
                outboxRepository.save(message);
                log.info("Successfully relayed message {}", message.getId());
            } catch (Exception e) {
                log.error("Failed to relay message {}", message.getId(), e);
            }
        }
    }
}
