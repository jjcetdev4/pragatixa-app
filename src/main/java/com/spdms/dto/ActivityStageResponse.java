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
    private Integer displayOrder;
    private StageStatus status;
    private Boolean isActive;
    private Boolean isUpcoming;
    private Boolean isCompleted;
    private String remainingTime;
    private String countdown;
    private List<ActivitySubgroupResponse> subgroups;
    private StageValidationResponse validation;
    private Boolean useDateValidation;
    private Boolean useThresholdValidation;
    private Boolean useCombinedValidation;
    private Boolean isVisible;
    private Boolean isLocked;

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

    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }

    public StageStatus getStatus() { return status; }
    public void setStatus(StageStatus status) { this.status = status; }

    public Boolean getIsActive() { return isActive; }
    public Boolean isActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsUpcoming() { return isUpcoming; }
    public Boolean isUpcoming() { return isUpcoming; }
    public void setIsUpcoming(Boolean isUpcoming) { this.isUpcoming = isUpcoming; }

    public Boolean getIsCompleted() { return isCompleted; }
    public Boolean isCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }

    public String getRemainingTime() { return remainingTime; }
    public void setRemainingTime(String remainingTime) { this.remainingTime = remainingTime; }

    public String getCountdown() { return countdown; }
    public void setCountdown(String countdown) { this.countdown = countdown; }

    public List<ActivitySubgroupResponse> getSubgroups() { return subgroups; }
    public void setSubgroups(List<ActivitySubgroupResponse> subgroups) { this.subgroups = subgroups; }

    public StageValidationResponse getValidation() { return validation; }
    public void setValidation(StageValidationResponse validation) { this.validation = validation; }

    public Boolean isUseDateValidation() { return useDateValidation; }
    public void setUseDateValidation(Boolean useDateValidation) { this.useDateValidation = useDateValidation; }

    public Boolean isUseThresholdValidation() { return useThresholdValidation; }
    public void setUseThresholdValidation(Boolean useThresholdValidation) { this.useThresholdValidation = useThresholdValidation; }

    public Boolean isUseCombinedValidation() { return useCombinedValidation; }
    public void setUseCombinedValidation(Boolean useCombinedValidation) { this.useCombinedValidation = useCombinedValidation; }

    public Boolean isVisible() { return isVisible; }
    public void setVisible(Boolean visible) { isVisible = visible; }

    public Boolean isLocked() { return isLocked; }
    public void setLocked(Boolean locked) { isLocked = locked; }

    private String stageStatus;
    
    public String getStageStatus() {
        return stageStatus;
    }

    public void setStageStatus(String stageStatus) {
        this.stageStatus = stageStatus;
    }
}
