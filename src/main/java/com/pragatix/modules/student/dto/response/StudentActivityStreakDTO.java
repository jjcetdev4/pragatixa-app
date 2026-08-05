package com.pragatix.modules.student.dto.response;

import java.time.LocalDate;

public class StudentActivityStreakDTO {
    private Long activityId;
    private String activityName;
    private int currentStreak;
    private int longestStreak;
    private LocalDate lastCompleted;

    public StudentActivityStreakDTO() {}

    public StudentActivityStreakDTO(Long activityId, String activityName, int currentStreak, int longestStreak, LocalDate lastCompleted) {
        this.activityId = activityId;
        this.activityName = activityName;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.lastCompleted = lastCompleted;
    }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int longestStreak) { this.longestStreak = longestStreak; }
    public LocalDate getLastCompleted() { return lastCompleted; }
    public void setLastCompleted(LocalDate lastCompleted) { this.lastCompleted = lastCompleted; }
}
