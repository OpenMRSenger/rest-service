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
                appointment.getId(),
                appointment.getPatientReference(),
                appointment.getDate(),
                appointment.getStatus()
        );
        appointmentRepository.save(appointmentEntity);

        OutboxMessageJpaEntity outboxEntity = new OutboxMessageJpaEntity(
                "appointment.events",
                eventPayload
        );
        outboxRepository.save(outboxEntity);
    }
}
