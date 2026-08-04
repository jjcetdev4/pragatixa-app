package com.pragatix.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a student team created by a Class Coordinator (CC) or for a Group
 * Activity
 */
@Entity
@Table(name = "teams", uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_name_class", columnNames = { "name", "department_id", "year", "section_id" })
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false)
    private int size;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "captain_id")
    private Student captain;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vice_captain_id")
    private Student viceCaptain;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "year")
    private String year;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id")
    private Section section;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "team", "teams" })
    private Set<Student> members = new HashSet<>();

    public Team() {
    }

    public Team(Long id, String name, int size, Student captain) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.captain = captain;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Student getCaptain() {
        return captain;
    }

    public void setCaptain(Student captain) {
        this.captain = captain;
    }

    public Student getViceCaptain() {
        return viceCaptain;
    }

    public void setViceCaptain(Student viceCaptain) {
        this.viceCaptain = viceCaptain;
    }

    public Set<Student> getMembers() {
        return members;
    }

    public void setMembers(Set<Student> members) {
        this.members = members;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Team team = new Team();

        public Builder name(String v) {
            team.name = v;
            return this;
        }

        public Builder size(int v) {
            team.size = v;
            return this;
        }

        public Builder captain(Student v) {
            team.captain = v;
            return this;
        }

        public Builder viceCaptain(Student v) {
            team.viceCaptain = v;
            return this;
        }

        public Builder department(Department v) {
            team.department = v;
            return this;
        }

        public Builder year(String v) {
            team.year = v;
            return this;
        }

        public Builder section(Section v) {
            team.section = v;
            return this;
        }

        public Builder createdBy(User v) {
            team.createdBy = v;
            return this;
        }

        public Builder members(Set<Student> v) {
            team.members = v;
            return this;
        }

        public Team build() {
            return team;
        }
    }
}
