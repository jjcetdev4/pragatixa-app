package com.pragatix.dto;

import java.time.LocalDateTime;

public class StreakResponse {
    private int currentStreak;
    private boolean isBroken;
    private LocalDateTime lastUpdated;
    private String streakType;
    private int penaltyPerBreak;

    public StreakResponse() {
    }

    public StreakResponse(int currentStreak, boolean isBroken, LocalDateTime lastUpdated, String streakType,
            int penaltyPerBreak) {
        this.currentStreak = currentStreak;
        this.isBroken = isBroken;
        this.lastUpdated = lastUpdated;
        this.streakType = streakType;
        this.penaltyPerBreak = penaltyPerBreak;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public boolean getIsBroken() {
        return isBroken;
    }

    public void setIsBroken(boolean isBroken) {
        this.isBroken = isBroken;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getStreakType() {
        return streakType;
    }

    public void setStreakType(String streakType) {
        this.streakType = streakType;
    }

    public int getPenaltyPerBreak() {
        return penaltyPerBreak;
    }

    public void setPenaltyPerBreak(int penaltyPerBreak) {
        this.penaltyPerBreak = penaltyPerBreak;
    }
}
