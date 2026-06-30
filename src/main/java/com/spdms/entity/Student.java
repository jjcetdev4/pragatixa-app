package com.spdms.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a student in the system
 */
@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String studentId;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String gender;

    private LocalDate dateOfBirth;

    @Column(length = 255)
    private String address;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(length = 20)
    private String semester;

    @Column(length = 20)
    private String academicYear;

    @Column(name = "year", length = 10)
    private String year;

    @Column(name = "section", length = 50)
    private String section;

    @Column(length = 50)
    private String sprNo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private int score = 100;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    public Student() {}

    // ── Getters & Setters ───────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getSprNo() { return sprNo; }
    public void setSprNo(String sprNo) { this.sprNo = sprNo; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Builder ─────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Student s = new Student();
        public Builder studentId(String v) { s.studentId = v; return this; }
        public Builder fullName(String v) { s.fullName = v; return this; }
        public Builder email(String v) { s.email = v; return this; }
        public Builder password(String v) { s.password = v; return this; }
        public Builder phone(String v) { s.phone = v; return this; }
        public Builder gender(String v) { s.gender = v; return this; }
        public Builder dateOfBirth(LocalDate v) { s.dateOfBirth = v; return this; }
        public Builder address(String v) { s.address = v; return this; }
        public Builder department(Department v) { s.department = v; return this; }
        public Builder semester(String v) { s.semester = v; return this; }
        public Builder academicYear(String v) { s.academicYear = v; return this; }
        public Builder year(String v) { s.year = v; return this; }
        public Builder section(String v) { s.section = v; return this; }
        public Builder sprNo(String v) { s.sprNo = v; return this; }
        public Builder active(boolean v) { s.active = v; return this; }
        public Builder score(int v) { s.score = v; return this; }
        public Builder group(Group v) { s.group = v; return this; }
        public Student build() { return s; }
    }
}
