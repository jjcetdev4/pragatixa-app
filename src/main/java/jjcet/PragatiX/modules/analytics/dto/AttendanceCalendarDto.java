package jjcet.PragatiX.modules.analytics.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public class AttendanceCalendarDto {
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
    private Long totalStudents;
    private Long presentCount;
    private Double attendanceRate;

    public AttendanceCalendarDto() {
    }

    public AttendanceCalendarDto(LocalDate date, Long totalStudents, Long presentCount, Double attendanceRate) {
        this.date = date;
        this.totalStudents = totalStudents;
        this.presentCount = presentCount;
        this.attendanceRate = attendanceRate;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Long getPresentCount() {
        return presentCount;
    }

    public void setPresentCount(Long presentCount) {
        this.presentCount = presentCount;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }
}