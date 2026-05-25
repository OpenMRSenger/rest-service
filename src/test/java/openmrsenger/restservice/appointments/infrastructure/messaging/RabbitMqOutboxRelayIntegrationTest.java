package openmrsenger.restservice.appointments.infrastructure.messaging;

import openmrsenger.restservice.appointments.infrastructure.persistence.OutboxMessageJpaEntity;
import openmrsenger.restservice.appointments.infrastructure.persistence.SpringDataOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * INTEGRATIE TEST: RabbitMqOutboxRelay
 * 
 * WAAROM DIT BETROUWBAARHEID BEWIJST:
 * Deze test simuleert de 'Relay' fase van het Outbox pattern. Het bewijst dat:
 * 1. Berichten die 'PENDING' zijn (niet geprocessed) correct worden opgepikt.
 * 2. Berichten succesvol worden gepubliceerd naar RabbitMQ.
 * 3. De status in de database pas naar 'processed = true' gaat NA succesvolle publicatie.
 * Dit mechanisme garandeert dat er geen berichten verloren gaan tussen de DB en de Broker.
 * 
 * UPDATE: Aangepast voor Spring Boot 4.0+ (@MockitoBean) en correcte entiteit constructie.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
class RabbitMqOutboxRelayIntegrationTest {

    @Autowired
    private SpringDataOutboxRepository outboxRepository;

    @Autowired
    private RabbitMqOutboxRelay outboxRelay;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private RabbitAdmin rabbitAdmin;

    @Test
    @DisplayName("Verifieert dat de relay scheduler berichten van de DB naar RabbitMQ verplaatst")
    void relayProcess_ShouldPublishAndMarkAsProcessed() {
        // Arrange
        String topic = "test.topic";
        String payload = "{\"event\":\"test\"}";
        LocalDateTime scheduledFor = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);
        
        // Gebruik de publieke constructor van de entiteit
        OutboxMessageJpaEntity message = new OutboxMessageJpaEntity(
                topic, 
                payload, 
                scheduledFor, 
                null, 
                UUID.randomUUID()
        );
        
        OutboxMessageJpaEntity savedMessage = outboxRepository.saveAndFlush(message);

        // Act
        outboxRelay.processOutbox();

        // Assert
        // 1. Verifieer dat RabbitTemplate is aangeroepen
        verify(rabbitTemplate).convertAndSend(eq(""), eq(topic), eq(payload));

        // 2. Verifieer dat de status in de database is bijgewerkt
        OutboxMessageJpaEntity processedMessage = outboxRepository.findById(savedMessage.getId()).orElseThrow();
        assertTrue(processedMessage.isProcessed(), "Het bericht moet als 'processed' gemarkeerd zijn in de database");
    }
}
