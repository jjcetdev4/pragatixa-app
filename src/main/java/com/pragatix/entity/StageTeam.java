package com.pragatix.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stage_teams", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stage_team", columnNames = { "stage_id", "team_id" })
})
public class StageTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stage_id", nullable = false)
    private ActivityStage stage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "captain_id")
    private Student captain;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vice_captain_id")
    private Student viceCaptain;

    public StageTeam() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActivityStage getStage() {
        return stage;
    }

    public void setStage(ActivityStage stage) {
        this.stage = stage;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
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
}
