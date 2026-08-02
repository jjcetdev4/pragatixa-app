package com.pragatix.modules.academiccalendar.dto;

import java.time.LocalDate;

public class AcademicWeekDto {
    private Long id;
    private Long academicMonthId;
    private Integer weekNumber;
    private LocalDate startDate;
    private LocalDate endDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAcademicMonthId() { return academicMonthId; }
    public void setAcademicMonthId(Long academicMonthId) { this.academicMonthId = academicMonthId; }
    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
