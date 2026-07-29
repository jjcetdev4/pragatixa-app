package com.pragatix.modules.attendance.dto.response;

import java.util.List;

public class AdminAttendanceSummaryResponse {
    private long totalStudents;
    private long totalPresent;
    private long totalAbsent;
    private double attendancePercentage;

    private List<StudentAttendanceListItemResponse> presentStudents;
    private List<StudentAttendanceListItemResponse> absentStudents;

    public AdminAttendanceSummaryResponse() {
    }

    public long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public long getTotalPresent() {
        return totalPresent;
    }

    public void setTotalPresent(long totalPresent) {
        this.totalPresent = totalPresent;
    }

    public long getTotalAbsent() {
        return totalAbsent;
    }

    public void setTotalAbsent(long totalAbsent) {
        this.totalAbsent = totalAbsent;
    }

    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public List<StudentAttendanceListItemResponse> getPresentStudents() {
        return presentStudents;
    }

    public void setPresentStudents(List<StudentAttendanceListItemResponse> presentStudents) {
        this.presentStudents = presentStudents;
    }

    public List<StudentAttendanceListItemResponse> getAbsentStudents() {
        return absentStudents;
    }

    public void setAbsentStudents(List<StudentAttendanceListItemResponse> absentStudents) {
        this.absentStudents = absentStudents;
    }
}
