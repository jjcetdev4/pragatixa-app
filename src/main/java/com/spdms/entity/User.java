package com.spdms.entity;

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

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, length = 150)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_sub_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "sub_role")
    private Set<String> subRoles = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "section", length = 50)
    private String section;

    @Column(name = "year", length = 10)
    private String year;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { this.createdAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { this.updatedAt = LocalDateTime.now(); }

    // ── Constructors ────────────────────────────────
    public User() {}

    // ── Getters & Setters ───────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public Set<String> getSubRoles() { return subRoles; }
    public void setSubRoles(Set<String> subRoles) { this.subRoles = subRoles; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ── Builder ─────────────────────────────────────
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final User user = new User();
        public Builder username(String v) { user.username = v; return this; }
        public Builder password(String v) { user.password = v; return this; }
        public Builder fullName(String v) { user.fullName = v; return this; }
        public Builder email(String v) { user.email = v; return this; }
        public Builder roles(Set<Role> v) { user.roles = v; return this; }
        public Builder subRoles(Set<String> v) { user.subRoles = v; return this; }
        public Builder department(Department v) { user.department = v; return this; }
        public Builder active(boolean v) { user.active = v; return this; }
        public Builder section(String v) { user.section = v; return this; }
        public Builder year(String v) { user.year = v; return this; }
        public User build() { return user; }
    }
}
