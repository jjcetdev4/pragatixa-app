package jjcet.PragatiX.modules.enrollment.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class SingleEnrollmentRequestDto {

    @NotBlank(message = "Student name is required")
    private String fullName;

    @NotBlank(message = "Gender is required")
    private String gender;

    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be exactly 10 digits")
    private String mobile;

    @NotNull(message = "Department is required")
    private Long departmentId;

    public SingleEnrollmentRequestDto() {}

    public SingleEnrollmentRequestDto(String fullName, String gender, String email, String mobile, Long departmentId) {
        this.fullName = fullName;
        this.gender = gender;
        this.email = email;
        this.mobile = mobile;
        this.departmentId = departmentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim().toUpperCase() : null;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
}
