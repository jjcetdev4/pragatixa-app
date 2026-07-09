package com.spdms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a student team created by a Class Coordinator (CC)
 */
@Entity
@Table(name = "teams")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false)
    private int size;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "captain_id")
    private Student captain;

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("team")
    private Set<Student> members = new HashSet<>();

    public Team() {}

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

    public Set<Student> getMembers() {
        return members;
    }

    public void setMembers(Set<Student> members) {
        this.members = members;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Team team = new Team();
        public Builder name(String v) { team.name = v; return this; }
        public Builder size(int v) { team.size = v; return this; }
        public Builder captain(Student v) { team.captain = v; return this; }
        public Team build() { return team; }
    }
}
