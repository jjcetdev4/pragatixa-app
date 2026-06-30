package com.spdms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Logs points added or deducted for discipline tracking
 */
@Entity
@Table(name = "discipline_logs")
public class DisciplineLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false, length = 255)
    private String reason;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subgroup_id")
    private ActivitySubgroup subgroup;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public DisciplineLog() {}

    public DisciplineLog(Student student, int points, String reason, ActivitySubgroup subgroup, User recordedBy) {
        this.student = student;
        this.points = points;
        this.reason = reason;
        this.subgroup = subgroup;
        this.recordedBy = recordedBy;
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

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public ActivitySubgroup getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(ActivitySubgroup subgroup) {
        this.subgroup = subgroup;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DisciplineLog log = new DisciplineLog();
        public Builder student(Student v) { log.student = v; return this; }
        public Builder points(int v) { log.points = v; return this; }
        public Builder reason(String v) { log.reason = v; return this; }
        public Builder subgroup(ActivitySubgroup v) { log.subgroup = v; return this; }
        public Builder recordedBy(User v) { log.recordedBy = v; return this; }
        public DisciplineLog build() { return log; }
    }
}
