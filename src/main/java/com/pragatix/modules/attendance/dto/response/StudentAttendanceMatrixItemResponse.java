package com.pragatix.modules.attendance.dto.response;

import java.util.Map;

public class StudentAttendanceMatrixItemResponse {
    private Long studentId;
    private String studentName;
    private String registerNumber;
    private Map<Integer, String> periodStatuses;

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

    public Map<Integer, String> getPeriodStatuses() {
        return periodStatuses;
    }

    public void setPeriodStatuses(Map<Integer, String> periodStatuses) {
        this.periodStatuses = periodStatuses;
    }
}
