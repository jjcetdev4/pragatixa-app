package com.pragatix.dto;

import java.util.List;

public class StudentTeamDetailsResponse {
    private Long teamId;
    private String teamName;
    private String stage;
    private String department;
    private String section;
    private String academicYear;
    private String semester;
    private String captainName;
    private String viceCaptainId;
    private String viceCaptainName;
    private String currentStudentRole;
    private int totalTeamXp;
    private int teamRank;
    private int maxTeamSize;
    private int currentMemberCount;
    private List<TeamMemberRankDto> members;

    public StudentTeamDetailsResponse() {
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

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
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

    public String getCurrentStudentRole() {
        return currentStudentRole;
    }

    public void setCurrentStudentRole(String currentStudentRole) {
        this.currentStudentRole = currentStudentRole;
    }

    public int getTotalTeamXp() {
        return totalTeamXp;
    }

    public void setTotalTeamXp(int totalTeamXp) {
        this.totalTeamXp = totalTeamXp;
    }

    public int getTeamRank() {
        return teamRank;
    }

    public void setTeamRank(int teamRank) {
        this.teamRank = teamRank;
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public void setMaxTeamSize(int maxTeamSize) {
        this.maxTeamSize = maxTeamSize;
    }

    public int getCurrentMemberCount() {
        return currentMemberCount;
    }

    public void setCurrentMemberCount(int currentMemberCount) {
        this.currentMemberCount = currentMemberCount;
    }

    public List<TeamMemberRankDto> getMembers() {
        return members;
    }

    public void setMembers(List<TeamMemberRankDto> members) {
        this.members = members;
    }
}
