package OpenMRSenger.rest_service.application;

import OpenMRSenger.rest_service.application.adapters.MessageAdapter;
import OpenMRSenger.rest_service.application.adapters.SwiftSendAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MessageManager {

    private final MessageAdapter messageAdapter;

    public MessageManager(
            @Value("${SWIFT_SEND_API_URL}") String apiUrl,
            @Value("${SWIFT_SEND_API_KEY}") String apiKey,
            @Value("${SWIFT_SEND_STUDENT_GROUP}") String studentGroup
    ) {
        this.messageAdapter = new SwiftSendAdapter(new RestTemplate(), apiUrl, apiKey, studentGroup);
    }

    public ResponseEntity<String> send(SendMessageCommand command) {
        return messageAdapter.send(command);
    }
}