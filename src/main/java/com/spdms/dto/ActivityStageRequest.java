package com.spdms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

public class ActivityStageRequest {

    @NotBlank(message = "Stage name is required")
    private String name;

    private String description;

    @Min(value = 0, message = "Expected XP cannot be negative")
    private Integer expectedXp;

    @NotNull(message = "Start date time is required")
    private LocalDateTime startDateTime;

    @NotNull(message = "End date time is required")
    private LocalDateTime endDateTime;

    private int displayOrder;

    private boolean useDateValidation = true;
    private boolean useThresholdValidation = false;
    private boolean useCombinedValidation = false;

    // Getters and Setters
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

    public boolean isUseDateValidation() { return useDateValidation; }
    public void setUseDateValidation(boolean useDateValidation) { this.useDateValidation = useDateValidation; }

    public boolean isUseThresholdValidation() { return useThresholdValidation; }
    public void setUseThresholdValidation(boolean useThresholdValidation) { this.useThresholdValidation = useThresholdValidation; }

    public boolean isUseCombinedValidation() { return useCombinedValidation; }
    public void setUseCombinedValidation(boolean useCombinedValidation) { this.useCombinedValidation = useCombinedValidation; }
}
