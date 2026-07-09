package com.spdms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "team_members", uniqueConstraints = {
    @UniqueConstraint(name = "uk_team_student", columnNames = {"team_id", "student_id"})
})
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "is_captain", nullable = false)
    private boolean isCaptain;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    public TeamMember() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public boolean isCaptain() { return isCaptain; }
    public void setCaptain(boolean captain) { isCaptain = captain; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final TeamMember tm = new TeamMember();
        public Builder student(Student v) { tm.student = v; return this; }
        public Builder isCaptain(boolean v) { tm.isCaptain = v; return this; }
        public Builder team(Team v) { tm.team = v; return this; }
        public TeamMember build() { return tm; }
    }
}
