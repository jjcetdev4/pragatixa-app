package com.pragatix.modules.activity.dto.response;

public class StageValidationResponse {
    private Boolean isVisible;
    private Boolean isLocked;
    private Boolean isCompleted;
    private Boolean isActive;
    private Boolean useDateValidation;
    private Boolean useThresholdValidation;
    private Boolean useCombinedValidation;

    public Boolean isVisible() {
        return isVisible;
    }

    public void setVisible(Boolean visible) {
        isVisible = visible;
    }

    public Boolean isLocked() {
        return isLocked;
    }

    public void setLocked(Boolean locked) {
        isLocked = locked;
    }

    public Boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(Boolean completed) {
        isCompleted = completed;
    }

    public Boolean isActive() {
        return isActive;
    }

    public void setActive(Boolean active) {
        isActive = active;
    }

    public Boolean isUseDateValidation() {
        return useDateValidation;
    }

    public void setUseDateValidation(Boolean useDateValidation) {
        this.useDateValidation = useDateValidation;
    }

    public Boolean isUseThresholdValidation() {
        return useThresholdValidation;
    }

    public void setUseThresholdValidation(Boolean useThresholdValidation) {
        this.useThresholdValidation = useThresholdValidation;
    }

    public Boolean isUseCombinedValidation() {
        return useCombinedValidation;
    }

    public void setUseCombinedValidation(Boolean useCombinedValidation) {
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
