package com.pragatix.modules.student.dto.response;

import java.util.List;

public class StudentProgressionDto {
    private int totalXp;
    private int currentLevel;
    private String currentLevelName;
    private int currentLevelMinXp;
    private int currentLevelMaxXp;
    private Integer nextLevel;
    private int remainingXp;
    private double progressPercentage;
    private List<LevelDto> unlockedLevels;
    private List<LevelDto> lockedLevels;
    private boolean isMaxLevel;

    public static class LevelDto {
        private int levelNumber;
        private String title;
        private int xpMin;
        private int xpMax;
        private int stage;
        private String primaryObjective;
        private String keyUnlocks;

        public LevelDto() {
        }

        public LevelDto(int levelNumber, String title, int xpMin, int xpMax, int stage, String primaryObjective,
                String keyUnlocks) {
            this.levelNumber = levelNumber;
            this.title = title;
            this.xpMin = xpMin;
            this.xpMax = xpMax;
            this.stage = stage;
            this.primaryObjective = primaryObjective;
            this.keyUnlocks = keyUnlocks;
        }

        public int getLevelNumber() {
            return levelNumber;
        }

        public void setLevelNumber(int levelNumber) {
            this.levelNumber = levelNumber;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getXpMin() {
            return xpMin;
        }

        public void setXpMin(int xpMin) {
            this.xpMin = xpMin;
        }

        public int getXpMax() {
            return xpMax;
        }

        public void setXpMax(int xpMax) {
            this.xpMax = xpMax;
        }

        public int getStage() {
            return stage;
        }

        public void setStage(int stage) {
            this.stage = stage;
        }

        public String getPrimaryObjective() {
            return primaryObjective;
        }

        public void setPrimaryObjective(String primaryObjective) {
            this.primaryObjective = primaryObjective;
        }

        public String getKeyUnlocks() {
            return keyUnlocks;
        }

        public void setKeyUnlocks(String keyUnlocks) {
            this.keyUnlocks = keyUnlocks;
        }
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getCurrentLevelName() {
        return currentLevelName;
    }

    public void setCurrentLevelName(String currentLevelName) {
        this.currentLevelName = currentLevelName;
    }

    public int getCurrentLevelMinXp() {
        return currentLevelMinXp;
    }

    public void setCurrentLevelMinXp(int currentLevelMinXp) {
        this.currentLevelMinXp = currentLevelMinXp;
    }

    public int getCurrentLevelMaxXp() {
        return currentLevelMaxXp;
    }

    public void setCurrentLevelMaxXp(int currentLevelMaxXp) {
        this.currentLevelMaxXp = currentLevelMaxXp;
    }

    public Integer getNextLevel() {
        return nextLevel;
    }

    public void setNextLevel(Integer nextLevel) {
        this.nextLevel = nextLevel;
    }

    public int getRemainingXp() {
        return remainingXp;
    }

    public void setRemainingXp(int remainingXp) {
        this.remainingXp = remainingXp;
    }

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public List<LevelDto> getUnlockedLevels() {
        return unlockedLevels;
    }

    public void setUnlockedLevels(List<LevelDto> unlockedLevels) {
        this.unlockedLevels = unlockedLevels;
    }

    public List<LevelDto> getLockedLevels() {
        return lockedLevels;
    }

    public void setLockedLevels(List<LevelDto> lockedLevels) {
        this.lockedLevels = lockedLevels;
    }

    public boolean getIsMaxLevel() {
        return isMaxLevel;
    }

    public void setIsMaxLevel(boolean maxLevel) {
        isMaxLevel = maxLevel;
    }
}
