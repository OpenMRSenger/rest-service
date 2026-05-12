package OpenMRSenger.restservice.application;

import OpenMRSenger.restservice.application.adapters.MessageAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for orchestrating message delivery.
 * It uses the Strategy Pattern to dynamically select the correct adapter at runtime.
 */
@Service
public class MessageManager {

    private static final Logger log = LoggerFactory.getLogger(MessageManager.class);
    private final List<MessageAdapter> adapters;

    /**
     * Spring automatically injects all beans that implement the MessageAdapter interface.
     */
    public MessageManager(List<MessageAdapter> adapters) {
        this.adapters = adapters;
    }

    /**
     * Dispatches the message to the appropriate adapter based on the command type.
     */
    public ResponseEntity<String> send(SendMessageCommand command) {
        log.info("Attempting to dispatch message of type: {}", command.getPrefProv());

        // Find the adapter that supports the requested type
        return adapters.stream()
                .filter(adapter -> adapter.supports(command.getPrefProv()))
                .findFirst()
                .map(adapter -> adapter.send(command))
                .orElseGet(() -> {
                    log.error("No suitable adapter found for type: {}", command.getPrefProv());
                    return ResponseEntity.badRequest()
                            .body("No provider found for message type: " + command.getPrefProv());
                });
    }
}