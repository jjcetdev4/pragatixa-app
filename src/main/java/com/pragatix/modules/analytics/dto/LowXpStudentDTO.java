package com.pragatix.modules.analytics.dto;

public class LowXpStudentDTO {
    private String studentName;
    private String registerNumber;
    private String department;
    private String section;
    private Long currentXp;
    private Long differenceFromThreshold;

    public LowXpStudentDTO() {}

    public LowXpStudentDTO(String studentName, String registerNumber, String department, String section, Long currentXp, Long differenceFromThreshold) {
        this.studentName = studentName;
        this.registerNumber = registerNumber;
        this.department = department;
        this.section = section;
        this.currentXp = currentXp;
        this.differenceFromThreshold = differenceFromThreshold;
    }

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

    public Long getDifferenceFromThreshold() { return differenceFromThreshold; }
    public void setDifferenceFromThreshold(Long differenceFromThreshold) { this.differenceFromThreshold = differenceFromThreshold; }
}
