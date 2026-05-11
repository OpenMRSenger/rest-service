package OpenMRSenger.rest_service.application;

import java.util.List;

public class SendMessageCommand {

        private final String prefProv;
        private final String type;
        private final List<String> recipients;
        private final String content;

        public SendMessageCommand(String prefProv, String type, List<String> recipients, String content) {
                this.prefProv = prefProv;
                this.type = type;
                this.recipients = recipients;
                this.content = content;
        }
        public String getPrefProv() {
                return prefProv;
        }
        public String getType() {
                return type;
        }

        public List<String> getRecipients() {
                return recipients;
        }

        public String getContent() {
                return content;
        }

}