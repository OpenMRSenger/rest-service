package openmrsenger.restservice.appointments.infrastructure.persistence;

import openmrsenger.restservice.appointments.domain.Appointment;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
@Transactional
public class AppointmentRepositoryAdapter implements AppointmentRepository {

    private final SpringDataAppointmentRepository appointmentRepository;
    private final SpringDataOutboxRepository outboxRepository;
    private static final ZoneId AMSTERDAM_ZONE = ZoneId.of("Europe/Amsterdam");

    public AppointmentRepositoryAdapter(SpringDataAppointmentRepository appointmentRepository,
            SpringDataOutboxRepository outboxRepository) {
        this.appointmentRepository = appointmentRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void save(Appointment appointment, String eventPayload) {
        AppointmentJpaEntity appointmentEntity = new AppointmentJpaEntity(
                appointment.id(),
                appointment.patientReference(),
                appointment.date(),
                appointment.status());
        appointmentRepository.save(appointmentEntity);

        saveToOutbox("appointment.events", eventPayload);
    }

    @Override
    public void saveToOutbox(String topic, String payload) {
        saveToOutbox(topic, payload, OffsetDateTime.now(AMSTERDAM_ZONE), null);
    }

    @Override
    public void saveToOutbox(String topic, String payload, OffsetDateTime scheduledFor, OffsetDateTime expiresAt) {
        OutboxMessageJpaEntity outboxEntity = new OutboxMessageJpaEntity(
                topic,
                payload,
                scheduledFor,
                expiresAt);
        outboxRepository.save(outboxEntity);
    }
}
