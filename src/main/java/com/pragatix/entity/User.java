package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a system user (Teacher, Admin, etc.)
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 150)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, length = 150)
    private String email;

    @Column(name = "phone", length = 15)
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

    @Column(name = "year", length = 10)
    private String year;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_year")
    private com.pragatix.enums.AcademicYear academicYear;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public com.pragatix.enums.AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(com.pragatix.enums.AcademicYear academicYear) {
        this.academicYear = academicYear;
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
        private final User user = new User();

        public Builder username(String v) {
            user.username = v;
            return this;
        }

        public Builder password(String v) {
            user.password = v;
            return this;
        }

        public Builder fullName(String v) {
            user.fullName = v;
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

        public Builder academicYear(com.pragatix.enums.AcademicYear v) {
            user.academicYear = v;
            return this;
        }

        public User build() {
            return user;
        }
    }
}
