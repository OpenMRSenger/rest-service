package openmrsenger.restservice.communications.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationLogAdapterTest {

    @Mock
    private SpringDataNotificationLogRepository repository;

    private NotificationLogAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new NotificationLogAdapter(repository);
    }

    @Test
    void isAlreadySent_True() {
        UUID id = UUID.randomUUID();
        NotificationLogJpaEntity entity = new NotificationLogJpaEntity(id);
        entity.setStatus("SENT");
        
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        
        assertTrue(adapter.isAlreadySent(id));
    }

    @Test
    void logPending_NewEntity() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        adapter.logPending(id, "TEST_PROVIDER", "HOSP-1");

        ArgumentCaptor<NotificationLogJpaEntity> captor = ArgumentCaptor.forClass(NotificationLogJpaEntity.class);
        verify(repository).save(captor.capture());
        assertEquals("PENDING", captor.getValue().getStatus());
        assertEquals("TEST_PROVIDER", captor.getValue().getProviderId());
        assertEquals("HOSP-1", captor.getValue().getHospitalId());
    }

    @Test
    void logSuccess_ExistingEntity() {
        UUID id = UUID.randomUUID();
        NotificationLogJpaEntity entity = new NotificationLogJpaEntity(id);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        adapter.logSuccess(id);

        verify(repository).save(entity);
        assertEquals("SENT", entity.getStatus());
    }
}
