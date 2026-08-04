package com.pragatix.modules.attendance.dto.response;

import com.pragatix.entity.AttendanceRecord;

public class StudentAttendanceListItemResponse {
    private Long studentId;
    private String studentName;
    private String registerNumber;
    private AttendanceRecord.AttendanceStatus status;
    private String remarks;

    public StudentAttendanceListItemResponse() {
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public void setRegisterNumber(String registerNumber) {
        this.registerNumber = registerNumber;
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
