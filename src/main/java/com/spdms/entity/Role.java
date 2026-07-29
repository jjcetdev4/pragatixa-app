package com.pragatix.entity;

import jakarta.persistence.*;

/**
 * Represents a role (ROLE_ADMIN, ROLE_TEACHER, ROLE_STUDENT)
 */
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    public Role() {
    }

    public Role(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Role role = new Role();

        public Builder name(String v) {
            role.name = v;
            return this;
        }

        public Role build() {
            return role;
        }
    }
}
