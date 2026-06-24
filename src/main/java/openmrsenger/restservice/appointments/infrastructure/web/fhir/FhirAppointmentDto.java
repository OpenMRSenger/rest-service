package openmrsenger.restservice.appointments.infrastructure.web.fhir;

import java.util.List;

public class FhirAppointmentDto {
    private String resourceType;
    private String id;
    private String status;
    private String start;
    private String end;
    private String description;
    private List<ParticipantDto> participant;
    private List<ContainedResourceDto> contained;

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }

    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<ParticipantDto> getParticipant() { return participant; }
    public void setParticipant(List<ParticipantDto> participant) { this.participant = participant; }

    public List<ContainedResourceDto> getContained() { return contained; }
    public void setContained(List<ContainedResourceDto> contained) { this.contained = contained; }

    public static class ParticipantDto {
        private ActorDto actor;
        private String status;
        private List<TelecomDto> telecom;

        public ActorDto getActor() { return actor; }
        public void setActor(ActorDto actor) { this.actor = actor; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public List<TelecomDto> getTelecom() { return telecom; }
        public void setTelecom(List<TelecomDto> telecom) { this.telecom = telecom; }
    }

    public static class ActorDto {
        private String reference;
        private String display;
        private List<TelecomDto> telecom;

        public String getReference() { return reference; }
        public void setReference(String reference) { this.reference = reference; }

        public String getDisplay() { return display; }
        public void setDisplay(String display) { this.display = display; }

        public List<TelecomDto> getTelecom() { return telecom; }
        public void setTelecom(List<TelecomDto> telecom) { this.telecom = telecom; }
    }

    public static class TelecomDto {
        private String system;
        private String value;

        public String getSystem() { return system; }
        public void setSystem(String system) { this.system = system; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    public static class ContainedResourceDto {
        private String resourceType;
        private String id;
        private List<TelecomDto> telecom;
        private String display;

        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public List<TelecomDto> getTelecom() { return telecom; }
        public void setTelecom(List<TelecomDto> telecom) { this.telecom = telecom; }

        public String getDisplay() { return display; }
        public void setDisplay(String display) { this.display = display; }
    }
}
