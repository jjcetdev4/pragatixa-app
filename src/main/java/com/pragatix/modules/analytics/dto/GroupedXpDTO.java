package com.pragatix.modules.analytics.dto;

public class GroupedXpDTO {
    private String groupName;
    private Double averageXp;
    private Long totalXp;
    private Long studentCount;

    public GroupedXpDTO() {}

    public GroupedXpDTO(String groupName, Double averageXp, Long totalXp, Long studentCount) {
        this.groupName = groupName;
        this.averageXp = averageXp;
        this.totalXp = totalXp;
        this.studentCount = studentCount;
    }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Double getAverageXp() { return averageXp; }
    public void setAverageXp(Double averageXp) { this.averageXp = averageXp; }

    public Long getTotalXp() { return totalXp; }
    public void setTotalXp(Long totalXp) { this.totalXp = totalXp; }

    public Long getStudentCount() { return studentCount; }
    public void setStudentCount(Long studentCount) { this.studentCount = studentCount; }
}
