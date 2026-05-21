package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import openmrsenger.restservice.appointments.infrastructure.messaging.RabbitMqTopology;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
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
    private final NotificationLogService notificationLogService;
    private final EventRetryService eventRetryService;

    public NotificationEventListener(List<MessagingProviderPort> providers,
                                     ObjectMapper objectMapper,
                                     NotificationLogService notificationLogService,
                                     EventRetryService eventRetryService) {
        this.providers = providers;
        this.objectMapper = objectMapper;
        this.notificationLogService = notificationLogService;
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

            // 1. Idempotency check using the new log service
            if (notificationLogService.isAlreadySent(event.getEventId())) {
                log.warn("Notification for event {} already successfully sent. Skipping.", event.getEventId());
                return;
            }

            // 2. Mark as PENDING before attempting to send (in a separate transaction if possible)
            notificationLogService.logPending(event.getEventId());

            log.info("Processing event {} for patient {} via provider {}", event.getEventId(), event.getPatientId(), event.getProviderId());

            MessagingProviderPort provider = providers.stream()
                    .filter(p -> p.supports(event.getProviderId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unsupported provider: " + event.getProviderId()));

            // 3. Attempt actual sending
            provider.send(event, event.getConfigurationJson());

            // 4. Mark as SENT after successful sending
            notificationLogService.logSuccess(event.getEventId());
            log.info("Successfully sent notification for patient {}", event.getPatientId());

        } catch (Exception e) {
            log.error("Failed to process notification event (retry stage {}): {}. Scheduling next retry.", 
                    retryStage, e.getMessage());
            
            // 5. Try to log the failure if we have the event ID
            try {
                NotificationRequestedEvent event = objectMapper.readValue(eventJson, NotificationRequestedEvent.class);
                notificationLogService.logFailure(event.getEventId(), e.getMessage());
            } catch (Exception jsonEx) {
                log.error("Could not log failure status because JSON is invalid", jsonEx);
            }
            
            eventRetryService.scheduleRetry(eventJson, retryStage, e);
        }
    }
}
