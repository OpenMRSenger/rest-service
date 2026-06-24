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
        ParticipantDto patient = findPatientParticipant();
        if (patient != null && patient.getActor() != null && patient.getActor().getReference() != null) {
            return patient.getActor().getReference().substring("Patient/".length());
        }
        return null;
    }

    public String getPatientName() {
        ParticipantDto patient = findPatientParticipant();
        if (patient != null && patient.getActor() != null) {
            return patient.getActor().getDisplay();
        }
        return null;
    }

    public String getArtsName() {
        if (participant == null) {
            return null;
        }
        for (ParticipantDto p : participant) {
            if (p != null && p.getActor() != null && p.getActor().getReference() != null && p.getActor().getReference().startsWith("Practitioner/")) {
                return p.getActor().getDisplay();
            }
        }
        return null;
    }

    public String getPhoneNumber() {
        ParticipantDto patient = findPatientParticipant();
        if (patient == null) {
            return null;
        }
        return extractPhoneFromParticipant(patient);
    }

    private ParticipantDto findPatientParticipant() {
        if (participant == null) {
            return null;
        }
        for (ParticipantDto p : participant) {
            if (p != null && p.getActor() != null && p.getActor().getReference() != null && p.getActor().getReference().startsWith("Patient/")) {
                return p;
            }
        }
        return null;
    }

    private String extractPhoneFromParticipant(ParticipantDto p) {
        String phone = extractPhoneFromTelecom(p.getTelecom());
        if (phone != null) {
            return phone;
        }
        if (p.getActor() != null) {
            return extractPhoneFromTelecom(p.getActor().getTelecom());
        }
        return null;
    }

    private String extractPhoneFromTelecom(List<TelecomDto> telecomList) {
        if (telecomList == null) {
            return null;
        }
        for (TelecomDto t : telecomList) {
            if (t != null && t.getValue() != null && isPhoneOrSms(t)) {
                return t.getValue();
            }
        }
        return null;
    }

    private boolean isPhoneOrSms(TelecomDto t) {
        String system = t.getSystem();
        return system == null || "phone".equalsIgnoreCase(system) || "sms".equalsIgnoreCase(system);
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
