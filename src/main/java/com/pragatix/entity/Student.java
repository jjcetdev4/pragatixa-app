package com.pragatix.entity;

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

    @Column(name = "reg_no", unique = true, length = 50)
    private String regNo;

    @Column(name = "spr_no", unique = true, length = 50)
    private String sprNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = true)
    private Section section;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "DOB")
    private LocalDate dobField;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "gender_id", nullable = false)
    private Gender genderRef;

    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYearRef;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "year_id", nullable = false)
    private Year yearRef;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semesterRef;

    @Column(name = "academic_year", length = 20)
    private String academicYear;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 255)
    private String address;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(length = 10)
    private String gender;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private int score = 100;

    @Column(length = 20)
    private String semester;

    @Column(length = 10)
    private String year;

    @Column(name = "total_xp", nullable = false)
    private int totalXp = 0;

    @Column(name = "group_xp", nullable = false)
    private int groupXp = 0;

    @Column(name = "individual_xp", nullable = false)
    private int individualXp = 0;

    @Column(name = "must_xp", nullable = false)
    private int mustXp = 0;

    @Column(name = "stage", nullable = false)
    private int stage = 1;

    @Column(name = "current_stage", nullable = false)
    private int currentStage = 1;

    @Transient
    private Long currentStageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "promotion_timestamp")
    private LocalDateTime promotionTimestamp;

    @Column(name = "promotion_order")
    private Integer promotionOrder;

    @Column(name = "is_captain", nullable = false)
    private boolean isCaptain = false;

    public Student() {
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

    public String getSprNo() {
        return sprNo;
    }

    public void setSprNo(String sprNo) {
        this.sprNo = sprNo;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDate getDobField() {
        return dobField;
    }

    public void setDobField(LocalDate dobField) {
        this.dobField = dobField;
    }

    public Gender getGenderRef() {
        return genderRef;
    }

    public void setGenderRef(Gender genderRef) {
        this.genderRef = genderRef;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public AcademicYear getAcademicYearRef() {
        return academicYearRef;
    }

    public void setAcademicYearRef(AcademicYear academicYearRef) {
        this.academicYearRef = academicYearRef;
    }

    public Year getYearRef() {
        return yearRef;
    }

    public void setYearRef(Year yearRef) {
        this.yearRef = yearRef;
    }

    public Semester getSemesterRef() {
        return semesterRef;
    }

    public void setSemesterRef(Semester semesterRef) {
        this.semesterRef = semesterRef;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public int getGroupXp() {
        return groupXp;
    }

    public void setGroupXp(int groupXp) {
        this.groupXp = groupXp;
    }

    public int getIndividualXp() {
        return individualXp;
    }

    public void setIndividualXp(int individualXp) {
        this.individualXp = individualXp;
    }

    public int getMustXp() {
        return mustXp;
    }

    public void setMustXp(int mustXp) {
        this.mustXp = mustXp;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
        this.currentStage = stage;
    }

    public int getCurrentStage() {
        return stage;
    }

    public void setCurrentStage(int currentStage) {
        this.stage = currentStage;
        this.currentStage = currentStage;
    }

    public Long getCurrentStageId() {
        return currentStageId;
    }

    public void setCurrentStageId(Long currentStageId) {
        this.currentStageId = currentStageId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getPromotionTimestamp() {
        return promotionTimestamp;
    }

    public void setPromotionTimestamp(LocalDateTime promotionTimestamp) {
        this.promotionTimestamp = promotionTimestamp;
    }

    public Integer getPromotionOrder() {
        return promotionOrder;
    }

    public void setPromotionOrder(Integer promotionOrder) {
        this.promotionOrder = promotionOrder;
    }

    public boolean isCaptain() {
        return isCaptain;
    }

    public void setCaptain(boolean isCaptain) {
        this.isCaptain = isCaptain;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Student s = new Student();

        public Builder regNo(String v) {
            s.regNo = v;
            return this;
        }

        public Builder sprNo(String v) {
            s.sprNo = v;
            return this;
        }

        public Builder isCaptain(boolean v) {
            s.isCaptain = v;
            return this;
        }

        public Builder department(Department v) {
            s.department = v;
            return this;
        }

        public Builder section(Section v) {
            s.section = v;
            return this;
        }

        public Builder user(User v) {
            s.user = v;
            return this;
        }

        public Builder dobField(LocalDate v) {
            s.dobField = v;
            return this;
        }

        public Builder genderRef(Gender v) {
            s.genderRef = v;
            return this;
        }

        public Builder phoneNo(String v) {
            s.phoneNo = v;
            return this;
        }

        public Builder academicYearRef(AcademicYear v) {
            s.academicYearRef = v;
            return this;
        }

        public Builder yearRef(Year v) {
            s.yearRef = v;
            return this;
        }

        public Builder semesterRef(Semester v) {
            s.semesterRef = v;
            return this;
        }

        public Builder academicYear(String v) {
            s.academicYear = v;
            return this;
        }

        public Builder active(boolean v) {
            s.active = v;
            return this;
        }

        public Builder address(String v) {
            s.address = v;
            return this;
        }

        public Builder dateOfBirth(LocalDate v) {
            s.dateOfBirth = v;
            return this;
        }

        public Builder email(String v) {
            s.email = v;
            return this;
        }

        public Builder fullName(String v) {
            s.fullName = v;
            return this;
        }

        public Builder gender(String v) {
            s.gender = v;
            return this;
        }

        public Builder password(String v) {
            s.password = v;
            return this;
        }

        public Builder phone(String v) {
            s.phone = v;
            return this;
        }

        public Builder score(int v) {
            s.score = v;
            return this;
        }

        public Builder semester(String v) {
            s.semester = v;
            return this;
        }

        public Builder year(String v) {
            s.year = v;
            return this;
        }

        public Builder team(Team v) {
            s.team = v;
            return this;
        }

        public Builder totalXp(int v) {
            s.totalXp = v;
            return this;
        }

        public Builder groupXp(int v) {
            s.groupXp = v;
            return this;
        }

        public Builder individualXp(int v) {
            s.individualXp = v;
            return this;
        }

        public Builder mustXp(int v) {
            s.mustXp = v;
            return this;
        }

        public Builder stage(int v) {
            s.stage = v;
            s.currentStage = v;
            return this;
        }

        public Builder currentStage(int v) {
            s.currentStage = v;
            s.stage = v;
            return this;
        }

        public Builder currentStageId(Long v) {
            s.currentStageId = v;
            return this;
        }

        public Student build() {
            return s;
        }
    }
}
