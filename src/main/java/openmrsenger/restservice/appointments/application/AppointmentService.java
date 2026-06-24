package openmrsenger.restservice.appointments.application;

public interface AppointmentService {
    void processWebhook(FhirAppointmentDto dto, String messagingProvider, String hospitalId, String providerConfigJson);
}

