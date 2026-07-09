package com.spdms.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StudentResponse {
    private Long id;
    private String studentId;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private LocalDate dateOfBirth;
    private String address;
    private String departmentName;
    private String semester;
    private String academicYear;
    private String year;
    private String section;
    private boolean active;
    private LocalDateTime createdAt;
    private String sprNo;
    private int score;
    private Long teamId;
    private String teamName;
    private boolean isCaptain;

    public StudentResponse() {}

    public boolean isCaptain() { return isCaptain; }
    public void setCaptain(boolean isCaptain) { this.isCaptain = isCaptain; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getSprNo() { return sprNo; }
    public void setSprNo(String sprNo) { this.sprNo = sprNo; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final StudentResponse r = new StudentResponse();
        public Builder id(Long v) { r.id = v; return this; }
        public Builder studentId(String v) { r.studentId = v; return this; }
        public Builder fullName(String v) { r.fullName = v; return this; }
        public Builder email(String v) { r.email = v; return this; }
        public Builder phone(String v) { r.phone = v; return this; }
        public Builder gender(String v) { r.gender = v; return this; }
        public Builder dateOfBirth(LocalDate v) { r.dateOfBirth = v; return this; }
        public Builder address(String v) { r.address = v; return this; }
        public Builder departmentName(String v) { r.departmentName = v; return this; }
        public Builder semester(String v) { r.semester = v; return this; }
        public Builder academicYear(String v) { r.academicYear = v; return this; }
        public Builder year(String v) { r.year = v; return this; }
        public Builder section(String v) { r.section = v; return this; }
        public Builder active(boolean v) { r.active = v; return this; }
        public Builder createdAt(LocalDateTime v) { r.createdAt = v; return this; }
        public Builder sprNo(String v) { r.sprNo = v; return this; }
        public Builder score(int v) { r.score = v; return this; }
        public Builder teamId(Long v) { r.teamId = v; return this; }
        public Builder teamName(String v) { r.teamName = v; return this; }
        public Builder isCaptain(boolean v) { r.isCaptain = v; return this; }
        public StudentResponse build() { return r; }
    }
}
