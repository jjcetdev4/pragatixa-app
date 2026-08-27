package jjcet.PragatiX.modules.superadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateYearAdminRequest {
    @Size(max = 100)
    private String username;

    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;

    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    private String phone;

    private boolean active = true;

    private Long assignedYearId;

    public CreateYearAdminRequest() {
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Long getAssignedYearId() {
        return assignedYearId;
    }

    public void setAssignedYearId(Long assignedYearId) {
        this.assignedYearId = assignedYearId;
    }
}
