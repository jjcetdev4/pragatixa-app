package com.pragatix.dto;

public class TeamMemberRankDto {
    private String profileImage;
    private String studentName;
    private String regNo;
    private String teamRole;
    private String currentStage;
    private String currentLevel;
    private int totalXp;
    private int rankInsideTeam;

    public TeamMemberRankDto() {
    }

    public TeamMemberRankDto(String profileImage, String studentName, String regNo, String teamRole,
            String currentStage, String currentLevel, int totalXp, int rankInsideTeam) {
        this.profileImage = profileImage;
        this.studentName = studentName;
        this.regNo = regNo;
        this.teamRole = teamRole;
        this.currentStage = currentStage;
        this.currentLevel = currentLevel;
        this.totalXp = totalXp;
        this.rankInsideTeam = rankInsideTeam;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public int getRankInsideTeam() {
        return rankInsideTeam;
    }

    public void setRankInsideTeam(int rankInsideTeam) {
        this.rankInsideTeam = rankInsideTeam;
    }
}
