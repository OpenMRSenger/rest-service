package openmrsenger.restservice.appointments.infrastructure.messaging;

import openmrsenger.restservice.appointments.infrastructure.persistence.OutboxMessageJpaEntity;
import openmrsenger.restservice.appointments.infrastructure.persistence.SpringDataOutboxRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RabbitMqOutboxRelay {

    private final SpringDataOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;

    public RabbitMqOutboxRelay(SpringDataOutboxRepository outboxRepository, RabbitTemplate rabbitTemplate) {
        this.outboxRepository = outboxRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<OutboxMessageJpaEntity> pendingMessages = outboxRepository.findByProcessedFalse();
        
        for (OutboxMessageJpaEntity message : pendingMessages) {
            try {
                rabbitTemplate.convertAndSend(message.getTopic(), message.getPayload());
                message.setProcessed(true);
                outboxRepository.save(message);
            } catch (Exception e) {
                // In production, implement retry logic or a dead letter queue
                System.err.println("Failed to publish message ID: " + message.getId());
            }
        }
    }
}
