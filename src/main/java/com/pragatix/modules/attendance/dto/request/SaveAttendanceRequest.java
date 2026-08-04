package com.pragatix.modules.attendance.dto.request;

import com.pragatix.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;

public class SaveAttendanceRequest {
    private LocalDate date;
    private Integer period;
    private Long academicYearId;
    private Long yearId;
    private Long departmentId;
    private Long sectionId;

    private List<StudentAttendanceRequest> records;

    public SaveAttendanceRequest() {
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

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public Long getYearId() {
        return yearId;
    }

    public void setYearId(Long yearId) {
        this.yearId = yearId;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public List<StudentAttendanceRequest> getRecords() {
        return records;
    }

    public void setRecords(List<StudentAttendanceRequest> records) {
        this.records = records;
    }

    public static class StudentAttendanceRequest {
        private Long studentId;
        private AttendanceRecord.AttendanceStatus status;
        private String remarks;

        public StudentAttendanceRequest() {
        }

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
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
}
