package jjcet.PragatiX.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Represents a system user (Teacher, Admin, etc.)
 */
@Entity
@Table(name = "users")
@Filter(name = "deletedFilter")
public class User implements SoftDeletable {
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanent_delete_at")
    private LocalDateTime permanentDeleteAt;

    @Column(name = "deleted_by")
    private String deletedBy;


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100)
    private String username;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Convert(converter = jjcet.PragatiX.util.crypto.AesGcmAttributeConverter.class)
    @Column(unique = true, length = 255)
    private String email;

    @Convert(converter = jjcet.PragatiX.util.crypto.AesGcmAttributeConverter.class)
    @Column(name = "phone", length = 255)
    private String phone;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_sub_roles", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "sub_role_id"))
    private Set<SubRole> subRoles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(name = "year", length = 50)
    private String year;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_year")
    private jjcet.PragatiX.enums.AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_year_id")
    private Year assignedYear;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public User() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim().toUpperCase() : null;
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

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public Set<SubRole> getSubRoles() {
        return subRoles;
    }

    public void setSubRoles(Set<SubRole> subRoles) {
        this.subRoles = subRoles;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public jjcet.PragatiX.enums.AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(jjcet.PragatiX.enums.AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public Year getAssignedYear() {
        return assignedYear;
    }

    public void setAssignedYear(Year assignedYear) {
        this.assignedYear = assignedYear;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    
    @Override
    public boolean isDeleted() { return deleted; }
    @Override
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    @Override
    public LocalDateTime getDeletedAt() { return deletedAt; }
    @Override
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    @Override
    public LocalDateTime getPermanentDeleteAt() { return permanentDeleteAt; }
    @Override
    public void setPermanentDeleteAt(LocalDateTime permanentDeleteAt) { this.permanentDeleteAt = permanentDeleteAt; }

    @Override
    public String getDeletedBy() { return deletedBy; }
    @Override
    public void setDeletedBy(String deletedBy) { this.deletedBy = deletedBy; }

public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final User user = new User();

        public Builder username(String v) {
            user.username = v;
            return this;
        }

        public Builder fullName(String v) {
            user.fullName = v != null ? v.trim().toUpperCase() : null;
            return this;
        }

        public Builder email(String v) {
            user.email = v;
            return this;
        }

        public Builder phone(String v) {
            user.phone = v;
            return this;
        }

        public Builder roles(Set<Role> v) {
            user.roles = v;
            return this;
        }

        public Builder subRoles(Set<SubRole> v) {
            user.subRoles = v;
            return this;
        }

        public Builder department(Department v) {
            user.department = v;
            return this;
        }

        public Builder active(boolean v) {
            user.active = v;
            return this;
        }

        public Builder section(Section v) {
            user.section = v;
            return this;
        }

        public Builder year(String v) {
            user.year = v;
            return this;
        }

        public Builder academicYear(jjcet.PragatiX.enums.AcademicYear v) {
            user.academicYear = v;
            return this;
        }

        public Builder assignedYear(Year v) {
            user.assignedYear = v;
            return this;
        }

        
        public Builder deleted(boolean v) {
            user.deleted = v;
            return this;
        }
        public Builder deletedAt(LocalDateTime v) {
            user.deletedAt = v;
            return this;
        }
        public Builder permanentDeleteAt(LocalDateTime v) {
            user.permanentDeleteAt = v;
            return this;
        }
        public Builder deletedBy(String v) {
            user.deletedBy = v;
            return this;
        }
public User build() {
            return user;
        }
    }
}
