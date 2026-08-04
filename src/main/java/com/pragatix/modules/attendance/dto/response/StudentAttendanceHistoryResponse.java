package com.pragatix.modules.attendance.dto.response;

import com.pragatix.entity.AttendanceRecord;
import java.time.LocalDate;

public class StudentAttendanceHistoryResponse {
    private LocalDate date;
    private Integer period;
    private AttendanceRecord.AttendanceStatus status;
    private String remarks;

    public StudentAttendanceHistoryResponse() {
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public AttendanceRecord.AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceRecord.AttendanceStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
