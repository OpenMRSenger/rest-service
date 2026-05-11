package OpenMRSenger.rest_service.application;

import OpenMRSenger.rest_service.application.adapters.AsyncFlowAdapter;
import OpenMRSenger.rest_service.application.adapters.MessageAdapter;
import OpenMRSenger.rest_service.application.adapters.SwiftSendAdapter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class MessageManager {

    private final MessageAdapter swiftSendAdapter;
    private final MessageAdapter asyncFlowAdapter;

    public MessageManager(

            @Value("${SWIFT_SEND_API_URL}") String swiftApiUrl,
            @Value("${SWIFT_SEND_API_KEY}") String swiftApiKey,
            @Value("${SWIFT_SEND_STUDENT_GROUP}") String swiftStudentGroup,

            @Value("${ASYNC_FLOW_API_URL}") String asyncApiUrl,
            @Value("${ASYNC_FLOW_API_KEY}") String asyncApiKey,
            @Value("${ASYNC_FLOW_STUDENT_GROUP}") String asyncStudentGroup

    ) {

        RestTemplate restTemplate = new RestTemplate();

        this.swiftSendAdapter = new SwiftSendAdapter(
                restTemplate,
                swiftApiUrl,
                swiftApiKey,
                swiftStudentGroup);

        this.asyncFlowAdapter = new AsyncFlowAdapter(
                restTemplate,
                asyncApiUrl,
                asyncApiKey,
                asyncStudentGroup);
    }

    public ResponseEntity<String> send(SendMessageCommand command) {

        if ("ASYNC".equalsIgnoreCase(command.type())) {
            return asyncFlowAdapter.send(command);
        }

        return swiftSendAdapter.send(command);
    }
}