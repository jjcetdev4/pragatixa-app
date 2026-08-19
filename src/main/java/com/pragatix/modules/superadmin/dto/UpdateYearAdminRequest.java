package jjcet.PragatiX.modules.superadmin.dto;

import jjcet.PragatiX.enums.AcademicYear;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateYearAdminRequest {
    @NotBlank(message = "Username is required")
    private String username;

    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @Email(message = "Invalid email format")
    private String email;

    private String phone;

    private Long assignedYearId;
    private boolean active;

    public UpdateYearAdminRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getAssignedYearId() {
        return assignedYearId;
    }

    public void setAssignedYearId(Long assignedYearId) {
        this.assignedYearId = assignedYearId;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
