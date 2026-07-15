package com.spdms.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.spdms.enums.StageStatus;

public class ActivityStageResponse {
    private Long id;
    private String name;
    private String description;
    private Integer expectedXp;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private int displayOrder;
    private StageStatus status;
    private boolean isActive;
    private boolean isUpcoming;
    private boolean isCompleted;
    private String remainingTime;
    private String countdown;
    private List<Map<String, Object>> subgroups;
    private StageValidationResponse validation;
    private boolean useDateValidation;
    private boolean useThresholdValidation;
    private boolean useCombinedValidation;
    private boolean isVisible;
    private boolean isLocked;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getExpectedXp() { return expectedXp; }
    public void setExpectedXp(Integer expectedXp) { this.expectedXp = expectedXp; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public StageStatus getStatus() { return status; }
    public void setStatus(StageStatus status) { this.status = status; }

    public boolean getIsActive() { return isActive; }
    public boolean isActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    public boolean getIsUpcoming() { return isUpcoming; }
    public boolean isUpcoming() { return isUpcoming; }
    public void setIsUpcoming(boolean isUpcoming) { this.isUpcoming = isUpcoming; }

    public boolean getIsCompleted() { return isCompleted; }
    public boolean isCompleted() { return isCompleted; }
    public void setIsCompleted(boolean isCompleted) { this.isCompleted = isCompleted; }

    public String getRemainingTime() { return remainingTime; }
    public void setRemainingTime(String remainingTime) { this.remainingTime = remainingTime; }

    public String getCountdown() { return countdown; }
    public void setCountdown(String countdown) { this.countdown = countdown; }

    public List<Map<String, Object>> getSubgroups() { return subgroups; }
    public void setSubgroups(List<Map<String, Object>> subgroups) { this.subgroups = subgroups; }

    public StageValidationResponse getValidation() { return validation; }
    public void setValidation(StageValidationResponse validation) { this.validation = validation; }

    public boolean isUseDateValidation() { return useDateValidation; }
    public void setUseDateValidation(boolean useDateValidation) { this.useDateValidation = useDateValidation; }

    public boolean isUseThresholdValidation() { return useThresholdValidation; }
    public void setUseThresholdValidation(boolean useThresholdValidation) { this.useThresholdValidation = useThresholdValidation; }

    public boolean isUseCombinedValidation() { return useCombinedValidation; }
    public void setUseCombinedValidation(boolean useCombinedValidation) { this.useCombinedValidation = useCombinedValidation; }

    public boolean isVisible() { return isVisible; }
    public void setVisible(boolean visible) { isVisible = visible; }

    public boolean isLocked() { return isLocked; }
    public void setLocked(boolean locked) { isLocked = locked; }

    private String stageStatus;
    
    public String getStageStatus() {
        return stageStatus;
    }

    public void setStageStatus(String stageStatus) {
        this.stageStatus = stageStatus;
    }
}
