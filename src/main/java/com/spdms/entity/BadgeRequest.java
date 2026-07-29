package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "badge_requests")
public class BadgeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "badge_id", nullable = false)
    private Badge badge;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, APPROVED, REJECTED

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "proof_link", columnDefinition = "TEXT")
    private String proofLink;

    public BadgeRequest() {
    }

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "PENDING";
        }
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Section getSection() {
        return section;
    }

    public void setSection(Section section) {
        this.section = section;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public String getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(String reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getProofLink() {
        return proofLink;
    }

    public void setProofLink(String proofLink) {
        this.proofLink = proofLink;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final BadgeRequest badgeRequest = new BadgeRequest();

        public Builder student(Student v) {
            badgeRequest.student = v;
            return this;
        }

        public Builder badge(Badge v) {
            badgeRequest.badge = v;
            return this;
        }

        public Builder department(Department v) {
            badgeRequest.department = v;
            return this;
        }

        public Builder section(Section v) {
            badgeRequest.section = v;
            return this;
        }

        public Builder status(String v) {
            badgeRequest.status = v;
            return this;
        }

        public Builder requestedAt(LocalDateTime v) {
            badgeRequest.requestedAt = v;
            return this;
        }

        public Builder reviewedAt(LocalDateTime v) {
            badgeRequest.reviewedAt = v;
            return this;
        }

        public Builder reviewedBy(String v) {
            badgeRequest.reviewedBy = v;
            return this;
        }

        public Builder remarks(String v) {
            badgeRequest.remarks = v;
            return this;
        }

        public Builder proofLink(String v) {
            badgeRequest.proofLink = v;
            return this;
        }

        public BadgeRequest build() {
            return badgeRequest;
        }
    }
}
