package openmrsenger.restservice.appointments.application;

import java.util.List;

public class ParticipantDto {
    private ActorDto actor;
    private String status;
    private List<TelecomDto> telecom;

    public ParticipantDto() {}

    public ParticipantDto(ActorDto actor, String status) {
        this.actor = actor;
        this.status = status;
    }

    public ParticipantDto(ActorDto actor, String status, List<TelecomDto> telecom) {
        this.actor = actor;
        this.status = status;
        this.telecom = telecom;
    }

    public ActorDto getActor() {
        return actor;
    }

    public void setActor(ActorDto actor) {
        this.actor = actor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public List<TelecomDto> getTelecom() {
        return telecom;
    }

    public void setTelecom(List<TelecomDto> telecom) {
        this.telecom = telecom;
    }
}
