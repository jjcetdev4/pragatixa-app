package jjcet.PragatiX.modules.enrollment.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollment_settings")
public class EnrollmentSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "enrollment_enabled", nullable = false)
    private boolean enrollmentEnabled = false;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public EnrollmentSetting() {}

    public EnrollmentSetting(Long id, boolean enrollmentEnabled, String updatedBy, LocalDateTime updatedAt) {
        this.id = id;
        this.enrollmentEnabled = enrollmentEnabled;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnrollmentEnabled() {
        return enrollmentEnabled;
    }

    public void setEnrollmentEnabled(boolean enrollmentEnabled) {
        this.enrollmentEnabled = enrollmentEnabled;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
