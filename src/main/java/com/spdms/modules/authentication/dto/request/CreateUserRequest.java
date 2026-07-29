package com.pragatix.modules.authentication.dto.request;

import jakarta.validation.constraints.*;
import java.util.Set;

public class CreateUserRequest {
    @NotBlank(message = "Username is required")
    @Size(max = 100)
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 4, message = "Password must be at least 4 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 255)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    private Long departmentId;

    private Set<String> roles; // e.g. ["ROLE_TEACHER"]

    private Set<String> subRoles; // e.g. ["HOD", "CC", "Subject: Mathematics"]

    private Long sectionId;
    private String year;

    public CreateUserRequest() {
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

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getSubRoles() {
        return subRoles;
    }

    public void setSubRoles(Set<String> subRoles) {
        this.subRoles = subRoles;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }
}
