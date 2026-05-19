package openmrsenger.restservice.appointments.application;

import java.time.LocalDateTime;

public class OpenMrsWebhookDto {
    private String event;
    private String appointmentUuid;
    private String patientUuid;
    private String patientName;
    private String artsName;
    private String status;
    private String phoneNumber;
    private String service;
    private String location;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String comments;

    // Getters and Setters
    public String getEvent() { return event; }
    public void setEvent(String event) { this.event = event; }

    public String getAppointmentUuid() { return appointmentUuid; }
    public void setAppointmentUuid(String appointmentUuid) { this.appointmentUuid = appointmentUuid; }

    public String getPatientUuid() { return patientUuid; }
    public void setPatientUuid(String patientUuid) { this.patientUuid = patientUuid; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getArtsName() { return artsName; }
    public void setArtsName(String artsName) { this.artsName = artsName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
