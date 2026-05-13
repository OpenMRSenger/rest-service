package openmrsenger.restservice.communications.domain;

public interface MessagingProviderPort {
    boolean supports(String providerId);
    void sendNotification(String patientId, String phoneNumber, String messageText, String apiKey);
}
