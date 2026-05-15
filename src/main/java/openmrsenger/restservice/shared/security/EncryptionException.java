package openmrsenger.restservice.shared.security;

public class EncryptionException extends RuntimeException {
    
    public EncryptionException(String message, Throwable cause) {
        super(message, cause);
    }
}
