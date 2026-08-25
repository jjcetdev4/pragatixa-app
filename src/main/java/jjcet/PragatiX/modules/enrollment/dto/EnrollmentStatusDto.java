package jjcet.PragatiX.modules.enrollment.dto;

public class EnrollmentStatusDto {
    private boolean enabled;

    public EnrollmentStatusDto() {}

    public EnrollmentStatusDto(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
