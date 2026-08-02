package com.pragatix.modules.academiccalendar.dto;

import java.time.LocalDate;

public class AcademicHolidayDto {
    private Long id;
    private Long academicMonthId;
    private String holidayName;
    private LocalDate holidayDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAcademicMonthId() { return academicMonthId; }
    public void setAcademicMonthId(Long academicMonthId) { this.academicMonthId = academicMonthId; }
    public String getHolidayName() { return holidayName; }
    public void setHolidayName(String holidayName) { this.holidayName = holidayName; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
}
