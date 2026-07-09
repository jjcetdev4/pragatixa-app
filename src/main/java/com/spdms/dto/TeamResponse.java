package com.spdms.dto;

import java.util.List;

public class TeamResponse {
    private Long teamId;
    private String teamName;
    private int teamCapacity;
    private String captainId;
    private String captainName;
    private List<StudentResponse> teamMembers;

    public TeamResponse() {}

    public TeamResponse(Long teamId, String teamName, int teamCapacity, String captainId, String captainName, List<StudentResponse> teamMembers) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.teamCapacity = teamCapacity;
        this.captainId = captainId;
        this.captainName = captainName;
        this.teamMembers = teamMembers;
    }

    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public int getTeamCapacity() { return teamCapacity; }
    public void setTeamCapacity(int teamCapacity) { this.teamCapacity = teamCapacity; }

    public String getCaptainId() { return captainId; }
    public void setCaptainId(String captainId) { this.captainId = captainId; }

    public String getCaptainName() { return captainName; }
    public void setCaptainName(String captainName) { this.captainName = captainName; }

    public List<StudentResponse> getTeamMembers() { return teamMembers; }
    public void setTeamMembers(List<StudentResponse> teamMembers) { this.teamMembers = teamMembers; }
}
