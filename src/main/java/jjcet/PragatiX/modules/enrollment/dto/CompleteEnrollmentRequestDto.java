package jjcet.PragatiX.modules.enrollment.dto;

import jakarta.validation.constraints.NotNull;

public class CompleteEnrollmentRequestDto {

    @NotNull(message = "Enrollment ID is required")
    private Long enrollmentId;

    public CompleteEnrollmentRequestDto() {}

    public CompleteEnrollmentRequestDto(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }

    public Long getEnrollmentId() {
        return enrollmentId;
    }

    public void setEnrollmentId(Long enrollmentId) {
        this.enrollmentId = enrollmentId;
    }
}
