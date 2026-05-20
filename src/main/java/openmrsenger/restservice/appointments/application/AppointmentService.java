package openmrsenger.restservice.appointments.application;

public interface AppointmentService {
    void processWebhook(OpenMrsWebhookDto dto, String messagingProvider, String hospitalId, String providerConfigJson);

}
