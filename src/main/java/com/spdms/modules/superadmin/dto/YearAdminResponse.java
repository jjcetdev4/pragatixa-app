package com.pragatix.modules.superadmin.dto;

import com.pragatix.enums.AcademicYear;

public class YearAdminResponse {
    private Long id;
    private String fullName;
    private String username;
    private AcademicYear academicYear;
    private boolean active;

    public YearAdminResponse() {
    }

    public YearAdminResponse(Long id, String fullName, String username, AcademicYear academicYear, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.academicYear = academicYear;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
