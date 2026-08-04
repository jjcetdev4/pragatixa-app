package com.pragatix.modules.analytics.dto;

public class XpAwardVsPenaltyDTO {
    private String departmentName;
    private Long awardXp;
    private Long penaltyXp;

    public XpAwardVsPenaltyDTO() {}

    public XpAwardVsPenaltyDTO(String departmentName, Long awardXp, Long penaltyXp) {
        this.departmentName = departmentName;
        this.awardXp = awardXp;
        this.penaltyXp = penaltyXp;
    }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getAwardXp() { return awardXp; }
    public void setAwardXp(Long awardXp) { this.awardXp = awardXp; }

    public Long getPenaltyXp() { return penaltyXp; }
    public void setPenaltyXp(Long penaltyXp) { this.penaltyXp = penaltyXp; }
}
