package com.pragatix.modules.analytics.dto;

public class AnalyticsOverviewDTO {
    private Double overallAttendancePercentage;
    private Integer presentStudents;
    private Integer partialAbsentees;
    private Integer fullDayAbsentees;
    private Integer totalStudents;

    public AnalyticsOverviewDTO() {
    }

    public AnalyticsOverviewDTO(Double overallAttendancePercentage, Integer presentStudents, Integer partialAbsentees, Integer fullDayAbsentees, Integer totalStudents) {
        this.overallAttendancePercentage = overallAttendancePercentage;
        this.presentStudents = presentStudents;
        this.partialAbsentees = partialAbsentees;
        this.fullDayAbsentees = fullDayAbsentees;
        this.totalStudents = totalStudents;
    }

    public Double getOverallAttendancePercentage() {
        return overallAttendancePercentage;
    }

    public void setOverallAttendancePercentage(Double overallAttendancePercentage) {
        this.overallAttendancePercentage = overallAttendancePercentage;
    }

    public Integer getPresentStudents() {
        return presentStudents;
    }

    public void setPresentStudents(Integer presentStudents) {
        this.presentStudents = presentStudents;
    }

    public Integer getPartialAbsentees() {
        return partialAbsentees;
    }

    public void setPartialAbsentees(Integer partialAbsentees) {
        this.partialAbsentees = partialAbsentees;
    }

    public Integer getFullDayAbsentees() {
        return fullDayAbsentees;
    }

    public void setFullDayAbsentees(Integer fullDayAbsentees) {
        this.fullDayAbsentees = fullDayAbsentees;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }
}
