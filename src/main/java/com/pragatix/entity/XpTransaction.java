package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "xp_transactions")
public class XpTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "team" })
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @Column(nullable = false, length = 50)
    private String category; // ACADEMIC, SKILL, LEADERSHIP, CAREER, INNOVATION, COMMUNITY, DISCIPLINE

    @Column(name = "activity_name", nullable = false, length = 255)
    private String activityName;

    @Column(name = "xp_points", nullable = false)
    private int xpPoints;

    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "is_penalty", nullable = false)
    private boolean isPenalty;

    @Column(name = "cap_applied", nullable = false)
    private boolean capApplied;

    @Column(name = "stage_order", columnDefinition = "int default 1")
    private Integer stage;

    public XpTransaction() {
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getXpPoints() {
        return xpPoints;
    }

    public void setXpPoints(int xpPoints) {
        this.xpPoints = xpPoints;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public boolean isPenalty() {
        return isPenalty;
    }

    public void setPenalty(boolean penalty) {
        isPenalty = penalty;
    }

    public boolean isCapApplied() {
        return capApplied;
    }

    public void setCapApplied(boolean capApplied) {
        this.capApplied = capApplied;
    }

    public Integer getStage() {
        return stage;
    }

    public void setStage(Integer stage) {
        this.stage = stage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final XpTransaction x = new XpTransaction();

        public Builder student(Student v) {
            x.student = v;
            return this;
        }

        public Builder activity(Activity v) {
            x.activity = v;
            return this;
        }

        public Builder category(String v) {
            x.category = v;
            return this;
        }

        public Builder activityName(String v) {
            x.activityName = v;
            return this;
        }

        public Builder xpPoints(int v) {
            x.xpPoints = v;
            return this;
        }

        public Builder evidenceUrl(String v) {
            x.evidenceUrl = v;
            return this;
        }

        public Builder submittedAt(LocalDateTime v) {
            x.submittedAt = v;
            return this;
        }

        public Builder status(String v) {
            x.status = v;
            return this;
        }

        public Builder approvedBy(String v) {
            x.approvedBy = v;
            return this;
        }

        public Builder isPenalty(boolean v) {
            x.isPenalty = v;
            return this;
        }

        public Builder capApplied(boolean v) {
            x.capApplied = v;
            return this;
        }

        public Builder stage(Integer v) {
            x.stage = v;
            return this;
        }

        public XpTransaction build() {
            return x;
        }
    }
}
