package com.pragatix.modules.analytics.dto;

public class XpTopPerformerDTO {
    private Integer rank;
    private String studentName;
    private String registerNumber;
    private String department;
    private String section;
    private Long currentXp;
    private Long awardedXp;
    private Long penaltyXp;

    public XpTopPerformerDTO() {}

    public XpTopPerformerDTO(Integer rank, String studentName, String registerNumber, String department, String section, Long currentXp, Long awardedXp, Long penaltyXp) {
        this.rank = rank;
        this.studentName = studentName;
        this.registerNumber = registerNumber;
        this.department = department;
        this.section = section;
        this.currentXp = currentXp;
        this.awardedXp = awardedXp;
        this.penaltyXp = penaltyXp;
    }

    // Getters and Setters
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getRegisterNumber() { return registerNumber; }
    public void setRegisterNumber(String registerNumber) { this.registerNumber = registerNumber; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public Long getCurrentXp() { return currentXp; }
    public void setCurrentXp(Long currentXp) { this.currentXp = currentXp; }

    public Long getAwardedXp() { return awardedXp; }
    public void setAwardedXp(Long awardedXp) { this.awardedXp = awardedXp; }

    public Long getPenaltyXp() { return penaltyXp; }
    public void setPenaltyXp(Long penaltyXp) { this.penaltyXp = penaltyXp; }
}
