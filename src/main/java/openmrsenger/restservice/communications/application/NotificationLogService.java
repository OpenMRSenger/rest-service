package openmrsenger.restservice.communications.application;

import java.util.UUID;

/**
 * Service for persistently tracking the status of notifications.
 */
public interface NotificationLogService {

    /**
     * Checks if a notification for the given event ID has already been successfully sent.
     *
     * @param eventId The unique ID of the event.
     * @return true if already sent, false otherwise.
     */
    boolean isAlreadySent(UUID eventId);

    /**
     * Logs the intent to send a notification. Status becomes PENDING.
     *
     * @param eventId The unique ID of the event.
     */
    void logPending(UUID eventId);

    /**
     * Logs that a notification has been successfully sent. Status becomes SENT.
     *
     * @param eventId The unique ID of the event.
     */
    void logSuccess(UUID eventId);

    /**
     * Logs that a notification sending attempt has failed. Status becomes FAILED.
     *
     * @param eventId The unique ID of the event.
     * @param error   The error message.
     */
    void logFailure(UUID eventId, String error);
}
