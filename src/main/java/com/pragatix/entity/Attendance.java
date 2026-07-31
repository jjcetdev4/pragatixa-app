package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance", uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_student_date_period", columnNames = { "student_id", "attendance_date",
                "period_no" })
})
public class Attendance {

    public enum AttendanceStatus {
        PRESENT, ABSENT, OD, LEAVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = true)
    private Subject subject;

    @Column(name = "reg_no", nullable = false)
    private String regNo;

    @Column(name = "attendance_date", nullable = false)
    private java.time.LocalDate attendanceDate;

    @Column(name = "period_no", nullable = false)
    private Integer periodNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @Column(length = 255)
    private String remarks;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Attendance() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Faculty getFaculty() {
        return faculty;
    }

    public void setFaculty(Faculty faculty) {
        this.faculty = faculty;
    }

    public Subject getSubject() {
        return subject;
    }

    public void setSubject(Subject subject) {
        this.subject = subject;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public java.time.LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(java.time.LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Integer getPeriodNo() {
        return periodNo;
    }

    public void setPeriodNo(Integer periodNo) {
        this.periodNo = periodNo;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Attendance a = new Attendance();

        public Builder student(Student v) {
            a.student = v;
            return this;
        }

        public Builder faculty(Faculty v) {
            a.faculty = v;
            return this;
        }

        public Builder subject(Subject v) {
            a.subject = v;
            return this;
        }

        public Builder regNo(String v) {
            a.regNo = v;
            return this;
        }

        public Builder attendanceDate(java.time.LocalDate v) {
            a.attendanceDate = v;
            return this;
        }

        public Builder periodNo(Integer v) {
            a.periodNo = v;
            return this;
        }

        public Builder status(AttendanceStatus v) {
            a.status = v;
            return this;
        }

        public Builder remarks(String v) {
            a.remarks = v;
            return this;
        }

        public Attendance build() {
            return a;
        }
    }
}
