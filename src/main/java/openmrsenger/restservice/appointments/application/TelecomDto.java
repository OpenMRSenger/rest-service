package openmrsenger.restservice.appointments.application;

public class TelecomDto {
    private String system;
    private String value;

    public TelecomDto() {}

    public TelecomDto(String system, String value) {
        this.system = system;
        this.value = value;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
