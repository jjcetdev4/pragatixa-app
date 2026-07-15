package com.spdms.dto;

public class StageValidationResponse {
    private boolean isVisible;
    private boolean isLocked;
    private boolean isCompleted;
    private boolean isActive;
    private boolean useDateValidation;
    private boolean useThresholdValidation;
    private boolean useCombinedValidation;

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isUseDateValidation() {
        return useDateValidation;
    }

    public void setUseDateValidation(boolean useDateValidation) {
        this.useDateValidation = useDateValidation;
    }

    public boolean isUseThresholdValidation() {
        return useThresholdValidation;
    }

    public void setUseThresholdValidation(boolean useThresholdValidation) {
        this.useThresholdValidation = useThresholdValidation;
    }

    public boolean isUseCombinedValidation() {
        return useCombinedValidation;
    }

    public void setUseCombinedValidation(boolean useCombinedValidation) {
        this.useCombinedValidation = useCombinedValidation;
    }

    private String stageStatus;

    public String getStageStatus() {
        return stageStatus;
    }

    public void setStageStatus(String stageStatus) {
        this.stageStatus = stageStatus;
    }
}
