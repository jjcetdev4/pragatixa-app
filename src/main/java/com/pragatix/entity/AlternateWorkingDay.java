package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alternate_working_day")
public class AlternateWorkingDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_calendar_month_id", nullable = false)
    private AcademicMonth academicMonth;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "original_holiday_day", nullable = false)
    private String originalHolidayDay;

    @Column(name = "working_day", nullable = false)
    private String workingDay;

    @Column(name = "reason")
    private String reason;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AcademicMonth getAcademicMonth() { return academicMonth; }
    public void setAcademicMonth(AcademicMonth academicMonth) { this.academicMonth = academicMonth; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getOriginalHolidayDay() { return originalHolidayDay; }
    public void setOriginalHolidayDay(String originalHolidayDay) { this.originalHolidayDay = originalHolidayDay; }

    public String getWorkingDay() { return workingDay; }
    public void setWorkingDay(String workingDay) { this.workingDay = workingDay; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
