package com.spdms.modules.admin.dto.response;

import com.spdms.entity.AssignedAcademicYear;

public class YearAdminResponse {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private AssignedAcademicYear assignedAcademicYear;
    private boolean active;

    public YearAdminResponse() {}

    public YearAdminResponse(Long id, String fullName, String username, String email, String phone, AssignedAcademicYear assignedAcademicYear, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.assignedAcademicYear = assignedAcademicYear;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public AssignedAcademicYear getAssignedAcademicYear() { return assignedAcademicYear; }
    public void setAssignedAcademicYear(AssignedAcademicYear assignedAcademicYear) { this.assignedAcademicYear = assignedAcademicYear; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
