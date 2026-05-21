package openmrsenger.restservice.appointments.infrastructure.persistence;

import openmrsenger.restservice.appointments.domain.Appointment;
import openmrsenger.restservice.appointments.domain.AppointmentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
        saveAppointment(appointment);
        saveToOutbox("appointment.events", eventPayload);
    }

    @Override
    public void saveAppointment(Appointment appointment) {
        appointmentRepository.save(new AppointmentJpaEntity(
                appointment.id(),
                appointment.patientReference(),
                appointment.date(),
                appointment.status()
        ));
    }

    @Override
    public Optional<Appointment> findAppointmentById(UUID id) {
        return appointmentRepository.findById(id)
                .map(e -> new Appointment(e.getId(), e.getPatientReference(), e.getDate(), e.getStatus()));
    }

    @Override
    public void saveToOutbox(String topic, String payload) {
        outboxRepository.save(new OutboxMessageJpaEntity(topic, payload, LocalDateTime.now(), null, null));
    }

    @Override
    public void saveToOutbox(String topic, String payload, LocalDateTime scheduledFor, LocalDateTime expiresAt, UUID eventId) {
        Optional<OutboxMessageJpaEntity> pending = eventId != null
                ? outboxRepository.findByEventIdAndProcessedFalseAndCancelledFalse(eventId)
                : Optional.empty();

        if (pending.isPresent()) {
            OutboxMessageJpaEntity existing = pending.get();
            if (!existing.getPayload().equals(payload)) {
                existing.setPayload(payload);
                existing.setScheduledFor(scheduledFor != null ? scheduledFor : LocalDateTime.now());
                existing.setExpiresAt(expiresAt);
                outboxRepository.save(existing);
            }
            return;
        }

        outboxRepository.save(new OutboxMessageJpaEntity(topic, payload, scheduledFor, expiresAt, eventId));
    }

    @Override
    public void cancelOutboxMessages(List<UUID> eventIds) {
        if (eventIds != null && !eventIds.isEmpty()) {
            outboxRepository.cancelByEventIds(eventIds);
        }
    }

    @Override
    public boolean wasNotificationSent(UUID eventId) {
        return outboxRepository.existsByEventIdAndProcessedTrue(eventId);
    }
}
