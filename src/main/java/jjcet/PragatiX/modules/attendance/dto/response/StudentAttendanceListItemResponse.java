package jjcet.PragatiX.modules.attendance.dto.response;

import jjcet.PragatiX.entity.AttendanceRecord;

public class StudentAttendanceListItemResponse {
    private Long studentId;
    private String studentName;
    private String registerNumber;
    private AttendanceRecord.AttendanceStatus status;
    private String remarks;
    private String markedByFacultyName;
    private String markedByFacultyDepartment;
    private String markedAt;

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

    public String getMarkedByFacultyName() {
        return markedByFacultyName;
    }

    public void setMarkedByFacultyName(String markedByFacultyName) {
        this.markedByFacultyName = markedByFacultyName;
    }

    public String getMarkedByFacultyDepartment() {
        return markedByFacultyDepartment;
    }

    public void setMarkedByFacultyDepartment(String markedByFacultyDepartment) {
        this.markedByFacultyDepartment = markedByFacultyDepartment;
    }

    public String getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(String markedAt) {
        this.markedAt = markedAt;
    }
}
