package com.pragatix.modules.analytics.dto;

import java.time.LocalDateTime;

public class XpHistoryDTO {
    private LocalDateTime date;
    private String studentName;
    private String registerNumber;
    private String department;
    private String section;
    private String activityName;
    private Integer awardXp;
    private Integer penaltyXp;
    private Integer netXp;
    private Long currentTotalXp;
    private String approvedBy;

    public XpHistoryDTO() {}

    public XpHistoryDTO(LocalDateTime date, String studentName, String registerNumber, String department, String section, String activityName, Integer awardXp, Integer penaltyXp, Integer netXp, Long currentTotalXp, String approvedBy) {
        this.date = date;
        this.studentName = studentName;
        this.registerNumber = registerNumber;
        this.department = department;
        this.section = section;
        this.activityName = activityName;
        this.awardXp = awardXp;
        this.penaltyXp = penaltyXp;
        this.netXp = netXp;
        this.currentTotalXp = currentTotalXp;
        this.approvedBy = approvedBy;
    }

    // Getters and Setters
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRegisterNumber() { return registerNumber; }
    public void setRegisterNumber(String registerNumber) { this.registerNumber = registerNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public Integer getAwardXp() { return awardXp; }
    public void setAwardXp(Integer awardXp) { this.awardXp = awardXp; }

    public Integer getPenaltyXp() { return penaltyXp; }
    public void setPenaltyXp(Integer penaltyXp) { this.penaltyXp = penaltyXp; }

    public Integer getNetXp() { return netXp; }
    public void setNetXp(Integer netXp) { this.netXp = netXp; }

    public Long getCurrentTotalXp() { return currentTotalXp; }
    public void setCurrentTotalXp(Long currentTotalXp) { this.currentTotalXp = currentTotalXp; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
}
