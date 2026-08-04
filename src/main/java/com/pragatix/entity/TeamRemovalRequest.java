package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "team_removal_requests")
public class TeamRemovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "captain_id", nullable = false)
    private Student captain;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public TeamRemovalRequest() {
    }

    public TeamRemovalRequest(Team team, Student student, Student captain, String reason, String status) {
        this.team = team;
        this.student = student;
        this.captain = captain;
        this.reason = reason;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Student getCaptain() {
        return captain;
    }

    public void setCaptain(Student captain) {
        this.captain = captain;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
