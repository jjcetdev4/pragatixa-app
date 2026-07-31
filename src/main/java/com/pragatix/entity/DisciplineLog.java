package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "discipline_logs")
public class DisciplineLog {

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
    @JoinColumn(name = "recorded_by")
    private Faculty recordedByFaculty;

    @Column(nullable = false)
    private int points;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "incident_date", nullable = false)
    private LocalDateTime incidentDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subgroup_id")
    private ActivitySubgroup subgroup;

    public DisciplineLog() {
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

    public Faculty getRecordedByFaculty() {
        return recordedByFaculty;
    }

    public void setRecordedByFaculty(Faculty recordedByFaculty) {
        this.recordedByFaculty = recordedByFaculty;
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
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

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(User recordedBy) {
        this.recordedBy = recordedBy;
    }

    public ActivitySubgroup getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(ActivitySubgroup subgroup) {
        this.subgroup = subgroup;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DisciplineLog log = new DisciplineLog();

        public Builder student(Student v) {
            log.student = v;
            return this;
        }

        public Builder activity(Activity v) {
            log.activity = v;
            return this;
        }

        public Builder recordedByFaculty(Faculty v) {
            log.recordedByFaculty = v;
            return this;
        }

        public Builder points(int v) {
            log.points = v;
            return this;
        }

        public Builder reason(String v) {
            log.reason = v;
            return this;
        }

        public Builder remarks(String v) {
            log.remarks = v;
            return this;
        }

        public Builder incidentDate(LocalDateTime v) {
            log.incidentDate = v;
            return this;
        }

        public Builder recordedBy(User v) {
            log.recordedBy = v;
            return this;
        }

        public Builder subgroup(ActivitySubgroup v) {
            log.subgroup = v;
            return this;
        }

        public DisciplineLog build() {
            return log;
        }
    }
}
