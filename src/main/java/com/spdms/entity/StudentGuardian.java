package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_guardians", uniqueConstraints = {
        @UniqueConstraint(name = "reg_no", columnNames = { "reg_no", "relationship" })
})
public class StudentGuardian {

    public enum RelationshipType {
        FATHER, MOTHER, GUARDIAN, PARENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "reg_no", nullable = false, length = 50)
    private String regNo;

    @Column(name = "guardian_name", nullable = false, length = 150)
    private String guardianName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType relationship;

    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @Column(length = 150)
    private String email;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public StudentGuardian() {
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

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public RelationshipType getRelationship() {
        return relationship;
    }

    public void setRelationship(RelationshipType relationship) {
        this.relationship = relationship;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(String phoneNo) {
        this.phoneNo = phoneNo;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
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
        private final StudentGuardian sg = new StudentGuardian();

        public Builder student(Student v) {
            sg.student = v;
            return this;
        }

        public Builder regNo(String v) {
            sg.regNo = v;
            return this;
        }

        public Builder guardianName(String v) {
            sg.guardianName = v;
            return this;
        }

        public Builder relationship(RelationshipType v) {
            sg.relationship = v;
            return this;
        }

        public Builder phoneNo(String v) {
            sg.phoneNo = v;
            return this;
        }

        public Builder email(String v) {
            sg.email = v;
            return this;
        }

        public Builder isPrimary(boolean v) {
            sg.isPrimary = v;
            return this;
        }

        public StudentGuardian build() {
            return sg;
        }
    }
}
