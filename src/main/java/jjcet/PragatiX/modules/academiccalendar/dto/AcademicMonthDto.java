package jjcet.PragatiX.modules.academiccalendar.dto;

import java.time.LocalDateTime;

public class AcademicMonthDto {
    private Long id;
    private Integer month;
    private Integer year;
    private jjcet.PragatiX.enums.AcademicYear academicYearEnum;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public jjcet.PragatiX.enums.AcademicYear getAcademicYearEnum() {
        return academicYearEnum;
    }

    public void setAcademicYearEnum(jjcet.PragatiX.enums.AcademicYear academicYearEnum) {
        this.academicYearEnum = academicYearEnum;
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
}
