package openmrsenger.restservice.appointments.infrastructure.messaging;

public final class RabbitMqTopology {

    public static final String MAIN_QUEUE = "appointment.events";
    public static final String RETRY_QUEUE_10S = "appointment.events.retry.10s";
    public static final String RETRY_QUEUE_60S = "appointment.events.retry.60s";
    public static final String RETRY_QUEUE_600S = "appointment.events.retry.600s";
    public static final String DLQ_QUEUE = "appointment.events.dlq";
    public static final String RETRY_STAGE_HEADER = "x-retry-stage";

    private RabbitMqTopology() {
    }
}