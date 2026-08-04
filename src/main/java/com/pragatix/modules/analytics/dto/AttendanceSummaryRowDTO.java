package com.pragatix.modules.analytics.dto;

public class AttendanceSummaryRowDTO {
    private String departmentName;
    private Integer present;
    private Integer partial;
    private Integer absent;
    private Double attendancePercentage;
    private Integer totalStudents;

    public AttendanceSummaryRowDTO() {
    }

    public AttendanceSummaryRowDTO(String departmentName, Integer present, Integer partial, Integer absent, Double attendancePercentage, Integer totalStudents) {
        this.departmentName = departmentName;
        this.present = present;
        this.partial = partial;
        this.absent = absent;
        this.attendancePercentage = attendancePercentage;
        this.totalStudents = totalStudents;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Integer getPresent() {
        return present;
    }

    public void setPresent(Integer present) {
        this.present = present;
    }

    public Integer getPartial() {
        return partial;
    }

    public void setPartial(Integer partial) {
        this.partial = partial;
    }

    public Integer getAbsent() {
        return absent;
    }

    public void setAbsent(Integer absent) {
        this.absent = absent;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public Integer getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Integer totalStudents) {
        this.totalStudents = totalStudents;
    }
}
