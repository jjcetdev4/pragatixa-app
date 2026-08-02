package com.pragatix.modules.academiccalendar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AlternateWorkingDayDto {
    private Long id;
    private Long academicMonthId;
    private LocalDate effectiveDate;
    private String originalHolidayDay;
    private String workingDay;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAcademicMonthId() { return academicMonthId; }
    public void setAcademicMonthId(Long academicMonthId) { this.academicMonthId = academicMonthId; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public String getOriginalHolidayDay() { return originalHolidayDay; }
    public void setOriginalHolidayDay(String originalHolidayDay) { this.originalHolidayDay = originalHolidayDay; }

    public String getWorkingDay() { return workingDay; }
    public void setWorkingDay(String workingDay) { this.workingDay = workingDay; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
