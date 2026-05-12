package OpenMRSenger.restservice.application.adapters;

import OpenMRSenger.restservice.application.SendMessageCommand;
import org.springframework.http.ResponseEntity;

public interface MessageAdapter {

    ResponseEntity<String> send(SendMessageCommand command);
    boolean supports(String type);
}
