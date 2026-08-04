package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subjects", uniqueConstraints = {
        @UniqueConstraint(name = "uq_subject_code", columnNames = { "dept_id", "sub_code" }),
        @UniqueConstraint(name = "uq_subject_name", columnNames = { "dept_id", "sub_name" }),
        @UniqueConstraint(name = "UKaodt3utnw0lsov4k9ta88dbpr", columnNames = { "name" })
})
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department;

    @Column(name = "sub_code", nullable = false, length = 20)
    private String subCode;

    @Column(name = "sub_name", nullable = false, length = 180)
    private String subName;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Subject() {
    }

    public Subject(String name) {
        this.name = name;
    }

    public Subject(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getSubCode() {
        return subCode;
    }

    public void setSubCode(String subCode) {
        this.subCode = subCode;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        private final Subject s = new Subject();

        public Builder department(Department v) {
            s.department = v;
            return this;
        }

        public Builder subCode(String v) {
            s.subCode = v;
            return this;
        }

        public Builder subName(String v) {
            s.subName = v;
            return this;
        }

        public Builder name(String v) {
            s.name = v;
            return this;
        }

        public Subject build() {
            return s;
        }
    }
}
