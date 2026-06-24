package openmrsenger.restservice.appointments.application;

import java.util.List;

public class ActorDto {
    private String reference;
    private String display;
    private List<TelecomDto> telecom;

    public ActorDto() {}

    public ActorDto(String reference, String display) {
        this.reference = reference;
        this.display = display;
    }

    public ActorDto(String reference, String display, List<TelecomDto> telecom) {
        this.reference = reference;
        this.display = display;
        this.telecom = telecom;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public List<TelecomDto> getTelecom() {
        return telecom;
    }

    public void setTelecom(List<TelecomDto> telecom) {
        this.telecom = telecom;
    }
}
