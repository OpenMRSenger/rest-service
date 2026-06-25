package openmrsenger.restservice.communications.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import openmrsenger.restservice.shared.messaging.RabbitMqConstants;
import openmrsenger.restservice.communications.domain.MessagingProviderPort;
import openmrsenger.restservice.shared.event.NotificationRequestedEvent;
import openmrsenger.restservice.shared.logging.LogSanitizer;
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
    private final MeterRegistry meterRegistry;

    public NotificationEventListener(List<MessagingProviderPort> providers,
                                     ObjectMapper objectMapper,
                                     NotificationLogService notificationLogService,
                                     EventRetryService eventRetryService,
                                     MeterRegistry meterRegistry) {
        this.providers = providers;
        this.objectMapper = objectMapper;
        this.notificationLogService = notificationLogService;
        this.eventRetryService = eventRetryService;
        this.meterRegistry = meterRegistry;
    }

    @RabbitListener(queues = RabbitMqConstants.MAIN_QUEUE)
    @Transactional
    public void handleNotificationEvent(
            String eventJson,
            @Header(name = RabbitMqConstants.RETRY_STAGE_HEADER, defaultValue = "0") int retryStage) {

        String providerId = "unknown";
        try {
            NotificationRequestedEvent event = objectMapper.readValue(eventJson, NotificationRequestedEvent.class);
            providerId = event.getProviderId();

            log.info("Received notification event (retry stage {}): eventId={}, patientId={}, providerId={}, hospitalId={}",
                    retryStage, event.getEventId(), event.getPatientId(), event.getProviderId(), event.getHospitalId());

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
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported provider: " + event.getProviderId()));

            // 3. Attempt actual sending
            provider.send(event, event.getConfigurationJson());

            // 4. Mark as SENT after successful sending
            notificationLogService.logSuccess(event.getEventId());
            log.info("Successfully sent notification for patient {}", event.getPatientId());
            meterRegistry.counter("notification_send", "provider", providerId, "outcome", "success").increment();

        } catch (Exception e) {
            log.error("Failed to process notification event (retry stage {}): {}. Scheduling next retry.",
                    retryStage, LogSanitizer.sanitizeExceptionMessage(e), e);

            // 5. Try to log the failure if we have the event ID
            try {
                NotificationRequestedEvent event = objectMapper.readValue(eventJson, NotificationRequestedEvent.class);
                notificationLogService.logFailure(event.getEventId(), LogSanitizer.sanitizeExceptionMessage(e));
            } catch (Exception jsonEx) {
                log.error("Could not log failure status because JSON is invalid: {}", LogSanitizer.sanitizeExceptionMessage(jsonEx), jsonEx);
            }

            meterRegistry.counter("notification_send", "provider", providerId, "outcome", "failure").increment();
            eventRetryService.scheduleRetry(eventJson, retryStage, e);
        }
    }
}
