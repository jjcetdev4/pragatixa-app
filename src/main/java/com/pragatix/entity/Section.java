package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "section", uniqueConstraints = {
        @UniqueConstraint(name = "uq_department_section", columnNames = { "dept_id", "section_name" })
})
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dept_id")
    private Department department;

    @Column(name = "section_name", nullable = false, length = 30)
    private String sectionName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Section() {
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

    @com.fasterxml.jackson.annotation.JsonProperty("departmentId")
    public Long getDepartmentId() {
        return department != null ? department.getId() : null;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
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
        private final Section s = new Section();

        public Builder department(Department v) {
            s.department = v;
            return this;
        }

        public Builder sectionName(String v) {
            s.sectionName = v;
            return this;
        }

        public Section build() {
            return s;
        }
    }
}
