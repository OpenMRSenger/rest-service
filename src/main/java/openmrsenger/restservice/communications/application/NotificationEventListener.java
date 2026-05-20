package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.communications.infrastructure.persistence.ProcessedNotificationJpaEntity;
import openmrsenger.restservice.communications.infrastructure.persistence.SpringDataProcessedNotificationRepository;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final List<MessagingProviderPort> providers;
    private final ObjectMapper objectMapper;
    private final SpringDataProcessedNotificationRepository processedNotificationRepository;

    public NotificationEventListener(List<MessagingProviderPort> providers, ObjectMapper objectMapper, SpringDataProcessedNotificationRepository processedNotificationRepository) {
        this.providers = providers;
        this.objectMapper = objectMapper;
        this.processedNotificationRepository = processedNotificationRepository;
    }

    @RabbitListener(queues = "appointment.events")
    @Transactional
    public void handleNotificationEvent(String eventJson) {
        log.info("Received notification event: {}", eventJson);
        try {
            NotificationRequestedEvent event = objectMapper.readValue(eventJson, NotificationRequestedEvent.class);
            
            if (processedNotificationRepository.existsById(event.getEventId())) {
                log.warn("Duplicate notification event detected: {}. Skipping.", event.getEventId());
                return;
            }

            log.info("Processing event {} for patient {} via provider {}", event.getEventId(), event.getPatientId(), event.getProviderId());

            MessagingProviderPort provider = providers.stream()
                    .filter(p -> p.supports(event.getProviderId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unsupported provider: " + event.getProviderId()));

            // Mark as processed BEFORE sending to avoid race conditions if RabbitMQ redelivers quickly
            processedNotificationRepository.save(new ProcessedNotificationJpaEntity(event.getEventId()));
            
            provider.send(event, event.getConfigurationJson());
            log.info("Successfully sent notification for patient {}", event.getPatientId());

        } catch (Exception e) {
            log.error("Failed to process notification event", e);
            // In a real scenario, we might want to throw to trigger RabbitMQ retry for transient errors
            // but we must be careful with the processed check then.
        }
    }
}
