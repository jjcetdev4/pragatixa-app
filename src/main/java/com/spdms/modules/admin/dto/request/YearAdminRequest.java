package com.spdms.modules.admin.dto.request;

import com.spdms.entity.AssignedAcademicYear;

public class YearAdminRequest {
    private String fullName;
    private String username;
    private String password;
    private String email;
    private String phone;
    private AssignedAcademicYear assignedAcademicYear;
    private boolean active;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public AssignedAcademicYear getAssignedAcademicYear() { return assignedAcademicYear; }
    public void setAssignedAcademicYear(AssignedAcademicYear assignedAcademicYear) { this.assignedAcademicYear = assignedAcademicYear; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
