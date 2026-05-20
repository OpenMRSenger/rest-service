package openmrsenger.restservice.appointments.infrastructure.persistence;

import openmrsenger.restservice.appointments.domain.Appointment;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import org.springframework.stereotype.Component;

@Component
public class AppointmentRepositoryAdapter implements AppointmentRepository {

    private final SpringDataAppointmentRepository appointmentRepository;
    private final SpringDataOutboxRepository outboxRepository;

    public AppointmentRepositoryAdapter(SpringDataAppointmentRepository appointmentRepository, SpringDataOutboxRepository outboxRepository) {
        this.appointmentRepository = appointmentRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void save(Appointment appointment, String eventPayload) {
        AppointmentJpaEntity appointmentEntity = new AppointmentJpaEntity(
                appointment.id(),
                appointment.patientReference(),
                appointment.date(),
                appointment.status()
        );
        appointmentRepository.save(appointmentEntity);

        saveToOutbox("appointment.events", eventPayload);
    }

    @Override
    public void saveToOutbox(String topic, String payload) {
        OutboxMessageJpaEntity outboxEntity = new OutboxMessageJpaEntity(
                topic,
                payload
        );
        outboxRepository.save(outboxEntity);
    }
}
