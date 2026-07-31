package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_points_history")
public class StudentPointsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "log_id")
    private DisciplineLog disciplineLog;

    @Column(nullable = false)
    private int points;

    @Column(name = "new_score", nullable = false)
    private int newScore;

    @Column(name = "new_streak", nullable = false)
    private int newStreak;

    @Column(name = "incident_date", nullable = false)
    private LocalDateTime incidentDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public StudentPointsHistory() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public DisciplineLog getDisciplineLog() {
        return disciplineLog;
    }

    public void setDisciplineLog(DisciplineLog disciplineLog) {
        this.disciplineLog = disciplineLog;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getNewScore() {
        return newScore;
    }

    public void setNewScore(int newScore) {
        this.newScore = newScore;
    }

    public int getNewStreak() {
        return newStreak;
    }

    public void setNewStreak(int newStreak) {
        this.newStreak = newStreak;
    }

    public LocalDateTime getIncidentDate() {
        return incidentDate;
    }

    public void setIncidentDate(LocalDateTime incidentDate) {
        this.incidentDate = incidentDate;
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
        private final StudentPointsHistory sph = new StudentPointsHistory();

        public Builder student(Student v) {
            sph.student = v;
            return this;
        }

        public Builder activity(Activity v) {
            sph.activity = v;
            return this;
        }

        public Builder disciplineLog(DisciplineLog v) {
            sph.disciplineLog = v;
            return this;
        }

        public Builder points(int v) {
            sph.points = v;
            return this;
        }

        public Builder newScore(int v) {
            sph.newScore = v;
            return this;
        }

        public Builder newStreak(int v) {
            sph.newStreak = v;
            return this;
        }

        public Builder incidentDate(LocalDateTime v) {
            sph.incidentDate = v;
            return this;
        }

        public StudentPointsHistory build() {
            return sph;
        }
    }
}
