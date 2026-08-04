package com.pragatix.modules.student.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.pragatix.modules.student.dto.request.GuardianDTO;

public class StudentResponse {
    private Long id;
    private String regNo;
    private String fullName;
    private String email;
    private String phone;
    private String gender;
    private Long genderId;
    private LocalDate dateOfBirth;
    private String address;
    private Long departmentId;
    private String departmentName;
    private String semester;
    private Long semesterId;
    private String academicYear;
    private Long academicYearId;
    private String year;
    private Long yearId;
    private String section;
    private Long sectionId;
    private String sectionName;
    private boolean active;
    private LocalDateTime createdAt;
    private String sprNo;
    private int score;
    private Long teamId;
    private String teamName;
    private String teamRole;
    private GuardianDTO guardian;

    public StudentResponse() {
    }

    public String getTeamRole() {
        return teamRole;
    }

    public void setTeamRole(String teamRole) {
        this.teamRole = teamRole;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Long getGenderId() {
        return genderId;
    }

    public void setGenderId(Long genderId) {
        this.genderId = genderId;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Long getYearId() {
        return yearId;
    }

    public void setYearId(Long yearId) {
        this.yearId = yearId;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSprNo() {
        return sprNo;
    }

    public void setSprNo(String sprNo) {
        this.sprNo = sprNo;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public GuardianDTO getGuardian() {
        return guardian;
    }

    public void setGuardian(GuardianDTO guardian) {
        this.guardian = guardian;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final StudentResponse r = new StudentResponse();

        public Builder id(Long v) {
            r.id = v;
            return this;
        }

        public Builder regNo(String v) {
            r.regNo = v;
            return this;
        }

        public Builder fullName(String v) {
            r.fullName = v;
            return this;
        }

        public Builder email(String v) {
            r.email = v;
            return this;
        }

        public Builder phone(String v) {
            r.phone = v;
            return this;
        }

        public Builder gender(String v) {
            r.gender = v;
            return this;
        }

        public Builder genderId(Long v) {
            r.genderId = v;
            return this;
        }

        public Builder dateOfBirth(LocalDate v) {
            r.dateOfBirth = v;
            return this;
        }

        public Builder address(String v) {
            r.address = v;
            return this;
        }

        public Builder departmentId(Long v) {
            r.departmentId = v;
            return this;
        }

        public Builder departmentName(String v) {
            r.departmentName = v;
            return this;
        }

        public Builder semester(String v) {
            r.semester = v;
            return this;
        }

        public Builder semesterId(Long v) {
            r.semesterId = v;
            return this;
        }

        public Builder academicYear(String v) {
            r.academicYear = v;
            return this;
        }

        public Builder academicYearId(Long v) {
            r.academicYearId = v;
            return this;
        }

        public Builder year(String v) {
            r.year = v;
            return this;
        }

        public Builder yearId(Long v) {
            r.yearId = v;
            return this;
        }

        public Builder section(String v) {
            r.section = v;
            return this;
        }

        public Builder sectionId(Long v) {
            r.sectionId = v;
            return this;
        }

        public Builder sectionName(String v) {
            r.sectionName = v;
            return this;
        }

        public Builder active(boolean v) {
            r.active = v;
            return this;
        }

        public Builder createdAt(LocalDateTime v) {
            r.createdAt = v;
            return this;
        }

        public Builder sprNo(String v) {
            r.sprNo = v;
            return this;
        }

        public Builder score(int v) {
            r.score = v;
            return this;
        }

        public Builder teamId(Long v) {
            r.teamId = v;
            return this;
        }

        public Builder teamName(String v) {
            r.teamName = v;
            return this;
        }

        public Builder teamRole(String v) {
            r.teamRole = v;
            return this;
        }

        public Builder guardian(GuardianDTO v) {
            r.guardian = v;
            return this;
        }

        public StudentResponse build() {
            return r;
        }
    }
}
