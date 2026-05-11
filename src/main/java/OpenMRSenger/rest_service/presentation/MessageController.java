package OpenMRSenger.rest_service.presentation;

import OpenMRSenger.rest_service.application.MessageManager;
import OpenMRSenger.rest_service.application.SendMessageCommand;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageManager messageManager;

    public MessageController(MessageManager messageManager) {
        this.messageManager = messageManager;
    }

    @PostMapping
    public ResponseEntity<String> send(@RequestBody SendMessageCommand command) {
        if (command == null || command.type() == null || command.type().isBlank()
                || command.recipients() == null || command.recipients().isEmpty()
                || command.content() == null || command.content().isBlank()) {
            return ResponseEntity.badRequest().body("Missing required field");
        }

        ResponseEntity<String> upstreamResponse = messageManager.send(command);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(upstreamResponse.getHeaders());

        if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }

        return ResponseEntity.status(upstreamResponse.getStatusCode()).headers(headers).body(upstreamResponse.getBody());
    }
}