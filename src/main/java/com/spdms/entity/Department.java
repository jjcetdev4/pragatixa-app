package com.spdms.entity;

import jakarta.persistence.*;

/**
 * Represents a school/college department
 */
@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 10)
    private String code;

    @Column(length = 255)
    private String description;

    public Department() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public static Builder builder() { return new Builder(); }
    public static class Builder {
        private final Department dept = new Department();
        public Builder name(String v) { dept.name = v; return this; }
        public Builder code(String v) { dept.code = v; return this; }
        public Builder description(String v) { dept.description = v; return this; }
        public Department build() { return dept; }
    }
}
