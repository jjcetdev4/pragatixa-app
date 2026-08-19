package jjcet.PragatiX.modules.superadmin.dto;

import jjcet.PragatiX.enums.AcademicYear;

public class YearAdminResponse {
    private Long id;
    private String fullName;
    private String username;
    private Long assignedYearId;
    private String assignedYearName;
    private boolean active;

    public YearAdminResponse() {
    }

    public YearAdminResponse(Long id, String fullName, String username, Long assignedYearId, String assignedYearName, boolean active) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.assignedYearId = assignedYearId;
        this.assignedYearName = assignedYearName;
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

    public Long getAssignedYearId() {
        return assignedYearId;
    }

    public void setAssignedYearId(Long assignedYearId) {
        this.assignedYearId = assignedYearId;
    }

    public String getAssignedYearName() {
        return assignedYearName;
    }

    public void setAssignedYearName(String assignedYearName) {
        this.assignedYearName = assignedYearName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
