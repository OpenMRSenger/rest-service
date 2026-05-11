package OpenMRSenger.rest_service.application;

import java.util.List;

public class SendMessageCommand {

        private final String type;
        private final List<String> recipients;
        private final String content;

        public SendMessageCommand(String type, List<String> recipients, String content) {
                this.type = type;
                this.recipients = recipients;
                this.content = content;
        }

        public String type() {
                return type;
        }

        public List<String> recipients() {
                return recipients;
        }

        public String content() {
                return content;
        }
}