package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import com.pragatix.enums.AcademicYear;

@Entity
@Table(name = "attendance_settings")
public class AttendanceSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_year")
    private AcademicYear academicYear;

    @Column(name = "daily_engine_enabled")
    private Boolean dailyEngineEnabled;

    @Column(name = "daily_processing_time")
    private LocalTime dailyProcessingTime;

    @Column(name = "weekly_engine_enabled")
    private Boolean weeklyEngineEnabled;

    @Column(name = "weekly_processing_time")
    private LocalTime weeklyProcessingTime;

    @Column(name = "partial_day_penalty")
    private Integer partialDayPenalty;

    @Column(name = "full_day_penalty")
    private Integer fullDayPenalty;

    @Column(name = "perfect_week_reward")
    private Integer perfectWeekReward;


    @Column(name = "week_start_full_penalty")
    private Integer weekStartFullPenalty;

    @Column(name = "week_start_partial_penalty")
    private Integer weekStartPartialPenalty;

    @Column(name = "week_end_full_penalty")
    private Integer weekEndFullPenalty;

    @Column(name = "week_end_partial_penalty")
    private Integer weekEndPartialPenalty;

    // --- Engine Control Center ---

    @Column(name = "test_mode_enabled")
    private Boolean testModeEnabled = false;

    @Column(name = "test_date")
    private LocalDate testDate;

    @Column(name = "test_time")
    private LocalTime testTime;

    @Column(name = "last_daily_run")
    private LocalDateTime lastDailyRun;

    @Column(name = "last_weekly_run")
    private LocalDateTime lastWeeklyRun;

    @Column(name = "daily_engine_status", length = 20)
    private String dailyEngineStatus = "WAITING";

    @Column(name = "weekly_engine_status", length = 20)
    private String weeklyEngineStatus = "WAITING";

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public Boolean getDailyEngineEnabled() {
        return dailyEngineEnabled;
    }

    public void setDailyEngineEnabled(Boolean dailyEngineEnabled) {
        this.dailyEngineEnabled = dailyEngineEnabled;
    }

    public LocalTime getDailyProcessingTime() {
        return dailyProcessingTime;
    }

    public void setDailyProcessingTime(LocalTime dailyProcessingTime) {
        this.dailyProcessingTime = dailyProcessingTime;
    }

    public Boolean getWeeklyEngineEnabled() {
        return weeklyEngineEnabled;
    }

    public void setWeeklyEngineEnabled(Boolean weeklyEngineEnabled) {
        this.weeklyEngineEnabled = weeklyEngineEnabled;
    }

    public LocalTime getWeeklyProcessingTime() {
        return weeklyProcessingTime;
    }

    public void setWeeklyProcessingTime(LocalTime weeklyProcessingTime) {
        this.weeklyProcessingTime = weeklyProcessingTime;
    }

    public Integer getPartialDayPenalty() {
        return partialDayPenalty;
    }

    public void setPartialDayPenalty(Integer partialDayPenalty) {
        this.partialDayPenalty = partialDayPenalty;
    }

    public Integer getFullDayPenalty() {
        return fullDayPenalty;
    }

    public void setFullDayPenalty(Integer fullDayPenalty) {
        this.fullDayPenalty = fullDayPenalty;
    }

    public Integer getPerfectWeekReward() {
        return perfectWeekReward;
    }

    public void setPerfectWeekReward(Integer perfectWeekReward) {
        this.perfectWeekReward = perfectWeekReward;
    }


    public Integer getWeekStartFullPenalty() {
        return weekStartFullPenalty;
    }

    public void setWeekStartFullPenalty(Integer weekStartFullPenalty) {
        this.weekStartFullPenalty = weekStartFullPenalty;
    }

    public Integer getWeekStartPartialPenalty() {
        return weekStartPartialPenalty;
    }

    public void setWeekStartPartialPenalty(Integer weekStartPartialPenalty) {
        this.weekStartPartialPenalty = weekStartPartialPenalty;
    }

    public Integer getWeekEndFullPenalty() {
        return weekEndFullPenalty;
    }

    public void setWeekEndFullPenalty(Integer weekEndFullPenalty) {
        this.weekEndFullPenalty = weekEndFullPenalty;
    }

    public Integer getWeekEndPartialPenalty() {
        return weekEndPartialPenalty;
    }

    public void setWeekEndPartialPenalty(Integer weekEndPartialPenalty) {
        this.weekEndPartialPenalty = weekEndPartialPenalty;
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

    // --- Engine Control Center Getters/Setters ---

    public Boolean getTestModeEnabled() {
        return testModeEnabled;
    }

    public void setTestModeEnabled(Boolean testModeEnabled) {
        this.testModeEnabled = testModeEnabled;
    }

    public java.time.LocalDate getTestDate() {
        return testDate;
    }

    public void setTestDate(java.time.LocalDate testDate) {
        this.testDate = testDate;
    }

    public LocalTime getTestTime() {
        return testTime;
    }

    public void setTestTime(LocalTime testTime) {
        this.testTime = testTime;
    }

    public LocalDateTime getLastDailyRun() {
        return lastDailyRun;
    }

    public void setLastDailyRun(LocalDateTime lastDailyRun) {
        this.lastDailyRun = lastDailyRun;
    }

    public LocalDateTime getLastWeeklyRun() {
        return lastWeeklyRun;
    }

    public void setLastWeeklyRun(LocalDateTime lastWeeklyRun) {
        this.lastWeeklyRun = lastWeeklyRun;
    }

    public String getDailyEngineStatus() {
        return dailyEngineStatus;
    }

    public void setDailyEngineStatus(String dailyEngineStatus) {
        this.dailyEngineStatus = dailyEngineStatus;
    }

    public String getWeeklyEngineStatus() {
        return weeklyEngineStatus;
    }

    public void setWeeklyEngineStatus(String weeklyEngineStatus) {
        this.weeklyEngineStatus = weeklyEngineStatus;
    }
}
