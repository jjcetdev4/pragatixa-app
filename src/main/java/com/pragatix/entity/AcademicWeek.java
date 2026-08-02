package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "academic_week")
public class AcademicWeek {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_month_id", nullable = false)
    private AcademicMonth academicMonth;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AcademicMonth getAcademicMonth() { return academicMonth; }
    public void setAcademicMonth(AcademicMonth academicMonth) { this.academicMonth = academicMonth; }
    public Integer getWeekNumber() { return weekNumber; }
    public void setWeekNumber(Integer weekNumber) { this.weekNumber = weekNumber; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}
