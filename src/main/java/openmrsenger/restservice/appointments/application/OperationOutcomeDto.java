package openmrsenger.restservice.appointments.application;

import java.util.ArrayList;
import java.util.List;

public class OperationOutcomeDto {
    private String resourceType = "OperationOutcome";
    private List<IssueDto> issue = new ArrayList<>();

    public OperationOutcomeDto() {}

    public OperationOutcomeDto(String severity, String code, String diagnostics) {
        addIssue(severity, code, diagnostics);
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public List<IssueDto> getIssue() {
        return issue;
    }

    public void setIssue(List<IssueDto> issue) {
        this.issue = issue;
    }

    public void addIssue(String severity, String code, String diagnostics) {
        this.issue.add(new IssueDto(severity, code, diagnostics));
    }

    public static class IssueDto {
        private String severity;
        private String code;
        private String diagnostics;

        public IssueDto() {}

        public IssueDto(String severity, String code, String diagnostics) {
            this.severity = severity;
            this.code = code;
            this.diagnostics = diagnostics;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDiagnostics() {
            return diagnostics;
        }

        public void setDiagnostics(String diagnostics) {
            this.diagnostics = diagnostics;
        }
    }
}
