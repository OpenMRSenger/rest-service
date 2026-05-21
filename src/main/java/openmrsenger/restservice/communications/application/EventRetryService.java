package openmrsenger.restservice.communications.application;

/**
 * Service responsible for scheduling retries when an event processing fails.
 */
public interface EventRetryService {

    /**
     * Schedules a retry for a failed event.
     *
     * @param eventJson         The original event JSON payload.
     * @param currentRetryStage The current retry stage (0 for the first attempt).
     * @param cause             The exception that caused the failure.
     */
    void scheduleRetry(String eventJson, int currentRetryStage, Exception cause);
}
