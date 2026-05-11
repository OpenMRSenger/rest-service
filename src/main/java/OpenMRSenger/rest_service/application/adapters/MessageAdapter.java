package OpenMRSenger.rest_service.application.adapters;

import OpenMRSenger.rest_service.application.SendMessageCommand;
import org.springframework.http.ResponseEntity;

public interface MessageAdapter {

    ResponseEntity<String> send(SendMessageCommand command);
}
