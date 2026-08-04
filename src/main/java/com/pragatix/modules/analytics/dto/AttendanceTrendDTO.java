package com.pragatix.modules.analytics.dto;

import java.time.LocalDate;

public class AttendanceTrendDTO {
    private LocalDate date;
    private Double attendancePercentage;

    public AttendanceTrendDTO() {
    }

    public AttendanceTrendDTO(LocalDate date, Double attendancePercentage) {
        this.date = date;
        this.attendancePercentage = attendancePercentage;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}
