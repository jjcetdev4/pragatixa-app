package com.pragatix.modules.attendancesettings.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import com.pragatix.enums.AcademicYear;

public class AttendanceSettingsDto {

    private Long id;
    private AcademicYear academicYear;
    private Boolean dailyEngineEnabled;
    private LocalTime dailyProcessingTime;
    private Boolean weeklyEngineEnabled;
    private LocalTime weeklyProcessingTime;
    private Integer partialDayPenalty;
    private Integer fullDayPenalty;
    private Integer perfectWeekReward;
    

    private Integer weekStartFullPenalty;
    private Integer weekStartPartialPenalty;
    private Integer weekEndFullPenalty;
    private Integer weekEndPartialPenalty;

    // Engine Control Center
    private Boolean testModeEnabled;
    private String testDate;       // "yyyy-MM-dd"
    private String testTime;       // "HH:mm:ss"
    private String lastDailyRun;   // ISO datetime string
    private String lastWeeklyRun;  // ISO datetime string
    private String dailyEngineStatus;
    private String weeklyEngineStatus;

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



    public Integer getWeekStartFullPenalty() { return weekStartFullPenalty; }
    public void setWeekStartFullPenalty(Integer weekStartFullPenalty) { this.weekStartFullPenalty = weekStartFullPenalty; }

    public Integer getWeekStartPartialPenalty() { return weekStartPartialPenalty; }
    public void setWeekStartPartialPenalty(Integer weekStartPartialPenalty) { this.weekStartPartialPenalty = weekStartPartialPenalty; }

    public Integer getWeekEndFullPenalty() { return weekEndFullPenalty; }
    public void setWeekEndFullPenalty(Integer weekEndFullPenalty) { this.weekEndFullPenalty = weekEndFullPenalty; }

    public Integer getWeekEndPartialPenalty() { return weekEndPartialPenalty; }
    public void setWeekEndPartialPenalty(Integer weekEndPartialPenalty) { this.weekEndPartialPenalty = weekEndPartialPenalty; }

    public Boolean getTestModeEnabled() { return testModeEnabled; }
    public void setTestModeEnabled(Boolean testModeEnabled) { this.testModeEnabled = testModeEnabled; }

    public String getTestDate() { return testDate; }
    public void setTestDate(String testDate) { this.testDate = testDate; }

    public String getTestTime() { return testTime; }
    public void setTestTime(String testTime) { this.testTime = testTime; }

    public String getLastDailyRun() { return lastDailyRun; }
    public void setLastDailyRun(String lastDailyRun) { this.lastDailyRun = lastDailyRun; }

    public String getLastWeeklyRun() { return lastWeeklyRun; }
    public void setLastWeeklyRun(String lastWeeklyRun) { this.lastWeeklyRun = lastWeeklyRun; }

    public String getDailyEngineStatus() { return dailyEngineStatus; }
    public void setDailyEngineStatus(String dailyEngineStatus) { this.dailyEngineStatus = dailyEngineStatus; }

    public String getWeeklyEngineStatus() { return weeklyEngineStatus; }
    public void setWeeklyEngineStatus(String weeklyEngineStatus) { this.weeklyEngineStatus = weeklyEngineStatus; }
}
