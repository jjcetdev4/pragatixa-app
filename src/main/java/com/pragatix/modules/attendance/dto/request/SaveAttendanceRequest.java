package jjcet.PragatiX.modules.attendance.dto.request;

import jjcet.PragatiX.entity.AttendanceRecord;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDate;
import java.util.List;

public class SaveAttendanceRequest {
    @NotNull(message = "date is required")
    private LocalDate date;

    @NotNull(message = "period is required")
    @Min(value = 1, message = "period must be between 1 and 8")
    @Max(value = 8, message = "period must be between 1 and 8")
    private Integer period;

    private Long academicYearId;

    @NotNull(message = "yearId is required")
    private Long yearId;

    @NotNull(message = "departmentId is required")
    private Long departmentId;

    private Long sectionId;

    @NotNull(message = "records is required")
    @Valid
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
        @NotNull(message = "studentId is required")
        private Long studentId;

        @NotNull(message = "status is required")
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
