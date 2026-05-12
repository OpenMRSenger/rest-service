package openmrsenger.restservice.application.adapters;

import openmrsenger.restservice.application.SendMessageCommand;
import org.springframework.http.ResponseEntity;

public interface MessageAdapter {

    ResponseEntity<String> send(SendMessageCommand command);
    boolean supports(String type);
}
