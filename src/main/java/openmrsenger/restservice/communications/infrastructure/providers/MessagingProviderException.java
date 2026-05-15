package openmrsenger.restservice.communications.infrastructure.providers;

public class MessagingProviderException extends RuntimeException {
    public MessagingProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public MessagingProviderException(String message) {
        super(message);
    }
}
