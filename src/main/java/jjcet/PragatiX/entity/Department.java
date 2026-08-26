package jjcet.PragatiX.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jjcet.PragatiX.enums.DepartmentType;

import org.hibernate.annotations.Filter;

/**
 * Represents a school/college department
 */
@Entity
@Table(name = "departments")
@Filter(name = "deletedFilter", condition = "deleted = false")
public class Department implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dept_code", nullable = false, unique = true, length = 20)
    private String deptCode;

    @Column(name = "dept_name", nullable = false, unique = true, length = 180)
    private String deptName;

    @Column(length = 10)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "department_type", length = 20)
    private DepartmentType departmentType = DepartmentType.MAIN;

    @Column(name = "supports_sections")
    private Boolean supportsSections;

    @JsonIgnore
    @OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Section> sections;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanent_delete_at")
    private LocalDateTime permanentDeleteAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    public Department() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeptCode() {
        return deptCode;
    }

    public void setDeptCode(String deptCode) {
        this.deptCode = deptCode;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DepartmentType getDepartmentType() {
        return departmentType != null ? departmentType : DepartmentType.MAIN;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public Boolean getSupportsSections() {
        return supportsSections;
    }

    public void setSupportsSections(Boolean supportsSections) {
        this.supportsSections = supportsSections;
    }

    public List<Section> getSections() {
        return sections;
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public LocalDateTime getPermanentDeleteAt() {
        return permanentDeleteAt;
    }

    @Override
    public void setPermanentDeleteAt(LocalDateTime permanentDeleteAt) {
        this.permanentDeleteAt = permanentDeleteAt;
    }

    @Override
    public String getDeletedBy() {
        return deletedBy;
    }

    @Override
    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Department dept = new Department();

        public Builder deptCode(String v) {
            dept.deptCode = v;
            return this;
        }

        public Builder deptName(String v) {
            dept.deptName = v;
            return this;
        }

        public Builder code(String v) {
            dept.code = v;
            return this;
        }

        public Builder name(String v) {
            dept.name = v;
            return this;
        }

        public Builder description(String v) {
            dept.description = v;
            return this;
        }

        public Builder departmentType(DepartmentType v) {
            dept.departmentType = v;
            return this;
        }

        public Builder supportsSections(Boolean v) {
            dept.supportsSections = v;
            return this;
        }

        public Department build() {
            return dept;
        }
    }
}
