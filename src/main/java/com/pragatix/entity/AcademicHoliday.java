package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "academic_holiday")
public class AcademicHoliday {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_month_id", nullable = false)
    private AcademicMonth academicMonth;

    @Column(name = "holiday_name", nullable = false)
    private String holidayName;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate holidayDate;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AcademicMonth getAcademicMonth() { return academicMonth; }
    public void setAcademicMonth(AcademicMonth academicMonth) { this.academicMonth = academicMonth; }
    public String getHolidayName() { return holidayName; }
    public void setHolidayName(String holidayName) { this.holidayName = holidayName; }
    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }
}
