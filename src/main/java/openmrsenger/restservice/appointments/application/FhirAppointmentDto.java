package openmrsenger.restservice.appointments.application;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;

public class FhirAppointmentDto {
    private String resourceType;
    private String id;
    private String status;
    private String start;
    private String end;
    private List<ParticipantDto> participant;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public List<ParticipantDto> getParticipant() {
        return participant;
    }

    public void setParticipant(List<ParticipantDto> participant) {
        this.participant = participant;
    }

    // Adaptor methods to retrieve fields previously supplied by OpenMrsWebhookDto:

    public String getAppointmentUuid() {
        return getId();
    }

    public String getPatientUuid() {
        if (participant == null) return null;
        for (ParticipantDto p : participant) {
            if (p.getActor() != null && p.getActor().getReference() != null) {
                String ref = p.getActor().getReference();
                if (ref.startsWith("Patient/")) {
                    return ref.substring("Patient/".length());
                }
            }
        }
        return null;
    }

    public String getPatientName() {
        if (participant == null) return null;
        for (ParticipantDto p : participant) {
            if (p.getActor() != null && p.getActor().getReference() != null && p.getActor().getReference().startsWith("Patient/")) {
                return p.getActor().getDisplay();
            }
        }
        return null;
    }

    public String getArtsName() {
        if (participant == null) return null;
        for (ParticipantDto p : participant) {
            if (p.getActor() != null && p.getActor().getReference() != null && p.getActor().getReference().startsWith("Practitioner/")) {
                return p.getActor().getDisplay();
            }
        }
        return null;
    }

    public String getPhoneNumber() {
        if (participant == null) return null;
        for (ParticipantDto p : participant) {
            if (p.getActor() != null && p.getActor().getReference() != null && p.getActor().getReference().startsWith("Patient/")) {
                // Check participant telecom
                if (p.getTelecom() != null) {
                    for (TelecomDto t : p.getTelecom()) {
                        if (t.getValue() != null && ("phone".equalsIgnoreCase(t.getSystem()) || "sms".equalsIgnoreCase(t.getSystem()) || t.getSystem() == null)) {
                            return t.getValue();
                        }
                    }
                }
                // Check actor telecom
                if (p.getActor().getTelecom() != null) {
                    for (TelecomDto t : p.getActor().getTelecom()) {
                        if (t.getValue() != null && ("phone".equalsIgnoreCase(t.getSystem()) || "sms".equalsIgnoreCase(t.getSystem()) || t.getSystem() == null)) {
                            return t.getValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    public OffsetDateTime getStartDateTime() {
        if (start == null) return null;
        try {
            return OffsetDateTime.parse(start);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public LocalDateTime getStartDateTimeUtc() {
        OffsetDateTime odt = getStartDateTime();
        return odt != null ? odt.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime() : null;
    }
}
