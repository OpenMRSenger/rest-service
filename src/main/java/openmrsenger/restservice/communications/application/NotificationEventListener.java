package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.infrastructure.messaging.RabbitMqTopology;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.communications.infrastructure.persistence.ProcessedNotificationJpaEntity;
import openmrsenger.restservice.communications.infrastructure.persistence.SpringDataProcessedNotificationRepository;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);
    private final List<MessagingProviderPort> providers;
    private final ObjectMapper objectMapper;
    private final SpringDataProcessedNotificationRepository processedNotificationRepository;
    private final EventRetryService eventRetryService;

    public NotificationEventListener(List<MessagingProviderPort> providers,
                                     ObjectMapper objectMapper,
                                     SpringDataProcessedNotificationRepository processedNotificationRepository,
                                     EventRetryService eventRetryService) {
        this.providers = providers;
        this.objectMapper = objectMapper;
        this.processedNotificationRepository = processedNotificationRepository;
        this.eventRetryService = eventRetryService;
    }

    @RabbitListener(queues = RabbitMqTopology.MAIN_QUEUE)
    @Transactional
    public void handleNotificationEvent(
            String eventJson,
            @Header(name = RabbitMqTopology.RETRY_STAGE_HEADER, defaultValue = "0") int retryStage) {

        log.info("Received notification event (retry stage {}): {}", retryStage, eventJson);
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

            // Actual sending
            provider.send(event, event.getConfigurationJson());

            // Mark as processed ONLY AFTER successful sending to allow retries on failure
            processedNotificationRepository.save(new ProcessedNotificationJpaEntity(event.getEventId()));
            log.info("Successfully sent notification for patient {}", event.getPatientId());

        } catch (Exception e) {
            log.error("Failed to process notification event (retry stage {}): {}. Scheduling next retry.", 
                    retryStage, e.getMessage());
            eventRetryService.scheduleRetry(eventJson, retryStage, e);
        }
    }
}
