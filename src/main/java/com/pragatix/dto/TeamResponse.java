package com.pragatix.dto;

import java.util.List;

import com.pragatix.modules.student.dto.response.StudentResponse;

public class TeamResponse {
    private Long teamId;
    private String teamName;
    private int teamCapacity;
    private String captainId;
    private String captainName;
    private String viceCaptainId;
    private String viceCaptainName;
    private List<StudentResponse> teamMembers;
    private Long assignmentId;
    private String assignmentName;
    private boolean canDelete;

    public TeamResponse() {
    }

    public TeamResponse(Long teamId, String teamName, int teamCapacity, String captainId, String captainName,
            String viceCaptainId, String viceCaptainName, List<StudentResponse> teamMembers) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamCapacity = teamCapacity;
        this.captainId = captainId;
        this.captainName = captainName;
        this.viceCaptainId = viceCaptainId;
        this.viceCaptainName = viceCaptainName;
        this.teamMembers = teamMembers;
        this.canDelete = false;
    }

    public TeamResponse(Long teamId, String teamName, int teamCapacity, String captainId, String captainName,
            String viceCaptainId, String viceCaptainName, List<StudentResponse> teamMembers, Long assignmentId,
            String assignmentName) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamCapacity = teamCapacity;
        this.captainId = captainId;
        this.captainName = captainName;
        this.viceCaptainId = viceCaptainId;
        this.viceCaptainName = viceCaptainName;
        this.teamMembers = teamMembers;
        this.assignmentId = assignmentId;
        this.assignmentName = assignmentName;
        this.canDelete = false;
    }

    public TeamResponse(Long teamId, String teamName, int teamCapacity, String captainId, String captainName,
            List<StudentResponse> teamMembers, Long assignmentId, String assignmentName) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamCapacity = teamCapacity;
        this.captainId = captainId;
        this.captainName = captainName;
        this.teamMembers = teamMembers;
        this.assignmentId = assignmentId;
        this.assignmentName = assignmentName;
        this.canDelete = false;
    }

    public TeamResponse(Long teamId, String teamName, int teamCapacity, String captainId, String captainName,
            List<StudentResponse> teamMembers, Long assignmentId, String assignmentName, boolean canDelete) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamCapacity = teamCapacity;
        this.captainId = captainId;
        this.captainName = captainName;
        this.teamMembers = teamMembers;
        this.assignmentId = assignmentId;
        this.assignmentName = assignmentName;
        this.canDelete = canDelete;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public int getTeamCapacity() {
        return teamCapacity;
    }

    public void setTeamCapacity(int teamCapacity) {
        this.teamCapacity = teamCapacity;
    }

    public String getCaptainId() {
        return captainId;
    }

    public void setCaptainId(String captainId) {
        this.captainId = captainId;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public String getViceCaptainId() {
        return viceCaptainId;
    }

    public void setViceCaptainId(String viceCaptainId) {
        this.viceCaptainId = viceCaptainId;
    }

    public String getViceCaptainName() {
        return viceCaptainName;
    }

    public void setViceCaptainName(String viceCaptainName) {
        this.viceCaptainName = viceCaptainName;
    }

    public List<StudentResponse> getTeamMembers() {
        return teamMembers;
    }

    public void setTeamMembers(List<StudentResponse> teamMembers) {
        this.teamMembers = teamMembers;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getAssignmentName() {
        return assignmentName;
    }

    public void setAssignmentName(String assignmentName) {
        this.assignmentName = assignmentName;
    }

    public boolean isCanDelete() {
        return canDelete;
    }

    public void setCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
    }

    private Long departmentId;
    private String departmentName;
    private Long academicYearId;
    private String academicYearName;
    private Long yearId;
    private String yearName;
    private Long semesterId;
    private String semesterName;
    private Long sectionId;
    private String sectionName;

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public String getAcademicYearName() {
        return academicYearName;
    }

    public void setAcademicYearName(String academicYearName) {
        this.academicYearName = academicYearName;
    }

    public Long getYearId() {
        return yearId;
    }

    public void setYearId(Long yearId) {
        this.yearId = yearId;
    }

    public String getYearName() {
        return yearName;
    }

    public void setYearName(String yearName) {
        this.yearName = yearName;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public String getSemesterName() {
        return semesterName;
    }

    public void setSemesterName(String semesterName) {
        this.semesterName = semesterName;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }
}
