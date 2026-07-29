package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_badges")
public class StudentBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @Column(name = "awarded_at")
    private LocalDateTime awardedAt;

    @Column(nullable = false, length = 50)
    private String status; // PENDING, APPROVED

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "evidence_url", columnDefinition = "TEXT")
    private String evidenceUrl;

    public StudentBadge() {
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

    public Badge getBadge() {
        return badge;
    }

    public void setBadge(Badge badge) {
        this.badge = badge;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
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

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final StudentBadge studentBadge = new StudentBadge();

        public Builder student(Student v) {
            studentBadge.student = v;
            return this;
        }

        public Builder badge(Badge v) {
            studentBadge.badge = v;
            return this;
        }

        public Builder awardedAt(LocalDateTime v) {
            studentBadge.awardedAt = v;
            return this;
        }

        public Builder status(String v) {
            studentBadge.status = v;
            return this;
        }

        public Builder approvedBy(String v) {
            studentBadge.approvedBy = v;
            return this;
        }

        public Builder evidenceUrl(String v) {
            studentBadge.evidenceUrl = v;
            return this;
        }

        public StudentBadge build() {
            return studentBadge;
        }
    }
}
