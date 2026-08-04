package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    public enum PriorityType {
        LOW, NORMAL, HIGH, URGENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityType priority = PriorityType.NORMAL;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "incident_date", nullable = false)
    private LocalDateTime incidentDate;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Notification() {
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PriorityType getPriority() {
        return priority;
    }

    public void setPriority(PriorityType priority) {
        this.priority = priority;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
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
        private final Notification n = new Notification();

        public Builder student(Student v) {
            n.student = v;
            return this;
        }

        public Builder title(String v) {
            n.title = v;
            return this;
        }

        public Builder message(String v) {
            n.message = v;
            return this;
        }

        public Builder priority(PriorityType v) {
            n.priority = v;
            return this;
        }

        public Builder referenceType(String v) {
            n.referenceType = v;
            return this;
        }

        public Builder referenceId(Long v) {
            n.referenceId = v;
            return this;
        }

        public Builder isRead(boolean v) {
            n.isRead = v;
            return this;
        }

        public Builder readAt(LocalDateTime v) {
            n.readAt = v;
            return this;
        }

        public Builder incidentDate(LocalDateTime v) {
            n.incidentDate = v;
            return this;
        }

        public Notification build() {
            return n;
        }
    }
}
