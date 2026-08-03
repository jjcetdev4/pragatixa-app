package com.pragatix.modules.analytics.dto;

import java.time.LocalDate;

public class AttendanceExportDTO {

    private String regNo;
    private String studentName;
    private String departmentName;
    private String sectionName;
    private LocalDate attendanceDate;
    private Integer period;
    private String status;

    public AttendanceExportDTO() {}

    public AttendanceExportDTO(String regNo, String studentName, String departmentName, String sectionName, LocalDate attendanceDate, Integer period, String status) {
        this.regNo = regNo;
        this.studentName = studentName;
        this.departmentName = departmentName;
        this.sectionName = sectionName;
        this.attendanceDate = attendanceDate;
        this.period = period;
        this.status = status;
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

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
