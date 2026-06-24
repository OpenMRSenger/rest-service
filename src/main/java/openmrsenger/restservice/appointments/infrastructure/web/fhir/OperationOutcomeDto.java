package openmrsenger.restservice.appointments.infrastructure.web.fhir;

import java.util.List;

public class OperationOutcomeDto {
    private String resourceType = "OperationOutcome";
    private List<IssueDto> issue;

    public OperationOutcomeDto() {}

    public OperationOutcomeDto(List<IssueDto> issue) {
        this.issue = issue;
    }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public List<IssueDto> getIssue() { return issue; }
    public void setIssue(List<IssueDto> issue) { this.issue = issue; }

    public static class IssueDto {
        private String severity; // fatal | error | warning | information
        private String code;     // e.g., structure, required, invalid, informational
        private DetailsDto details;
        private List<String> expression;

        public IssueDto() {}

        public IssueDto(String severity, String code, String text) {
            this.severity = severity;
            this.code = code;
            this.details = new DetailsDto(text);
        }

        public IssueDto(String severity, String code, String text, List<String> expression) {
            this.severity = severity;
            this.code = code;
            this.details = new DetailsDto(text);
            this.expression = expression;
        }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }

        public DetailsDto getDetails() { return details; }
        public void setDetails(DetailsDto details) { this.details = details; }

        public List<String> getExpression() { return expression; }
        public void setExpression(List<String> expression) { this.expression = expression; }
    }

    public static class DetailsDto {
        private String text;

        public DetailsDto() {}

        public DetailsDto(String text) {
            this.text = text;
        }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}
