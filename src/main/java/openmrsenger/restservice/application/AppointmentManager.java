package openmrsenger.restservice.application;

import openmrsenger.restservice.dal.AppointmentRepository;
import openmrsenger.restservice.dal.OutboxRepository;
import openmrsenger.restservice.domain.Appointment;
import openmrsenger.restservice.domain.OutboxMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentManager {

    private final AppointmentRepository appointmentRepository;
    private final OutboxRepository outboxRepository;

    public AppointmentManager(AppointmentRepository appointmentRepository, OutboxRepository outboxRepository) {
        this.appointmentRepository = appointmentRepository;
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public void scheduleAppointmentAndEmitEvent(Appointment appointment, String eventPayload) {
        appointmentRepository.save(appointment);

        OutboxMessage outboxMessage = new OutboxMessage(eventPayload, "appointment.created.topic");
        outboxRepository.save(outboxMessage);
    }
}