package com.pragatix.dto;

import java.time.LocalDateTime;

public class TeamRemovalRequestDto {
    private Long id;
    private Long teamId;
    private String teamName;
    private String regNo;
    private String studentName;
    private String captainId;
    private String captainName;
    private String reason;
    private String status;
    private LocalDateTime createdAt;

    public TeamRemovalRequestDto() {
    }

    public TeamRemovalRequestDto(Long id, Long teamId, String teamName, String regNo, String studentName,
            String captainId, String captainName, String reason, String status, LocalDateTime createdAt) {
        this.id = id;
        this.teamId = teamId;
        this.teamName = teamName;
        this.regNo = regNo;
        this.studentName = studentName;
        this.captainId = captainId;
        this.captainName = captainName;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
