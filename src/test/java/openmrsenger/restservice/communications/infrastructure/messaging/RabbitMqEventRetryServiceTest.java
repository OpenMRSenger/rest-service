package openmrsenger.restservice.communications.infrastructure.messaging;

import openmrsenger.restservice.appointments.infrastructure.messaging.RabbitMqTopology;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitMqEventRetryServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private RabbitMqEventRetryService retryService;

    @BeforeEach
    void setUp() {
        retryService = new RabbitMqEventRetryService(rabbitTemplate);
    }

    @Test
    void scheduleRetry_Stage0To1() {
        // Arrange
        String json = "{}";
        
        // Act
        retryService.scheduleRetry(json, 0, new RuntimeException());

        // Assert
        ArgumentCaptor<MessagePostProcessor> mppCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMqTopology.RETRY_QUEUE_10S), eq((Object)json), mppCaptor.capture());
        
        Message message = mock(Message.class);
        MessageProperties props = new MessageProperties();
        when(message.getMessageProperties()).thenReturn(props);
        
        mppCaptor.getValue().postProcessMessage(message);
        assertEquals(1, props.getHeaders().get(RabbitMqTopology.RETRY_STAGE_HEADER));
    }

    @Test
    void scheduleRetry_MaxReached_GoesToDlq() {
        // Arrange
        String json = "{}";
        
        // Act
        retryService.scheduleRetry(json, 3, new RuntimeException());

        // Assert
        verify(rabbitTemplate).convertAndSend(eq(RabbitMqTopology.DLQ_QUEUE), eq((Object)json), any(MessagePostProcessor.class));
    }
}
