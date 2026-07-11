package com.spdms.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class ActivityStageResponse {
    private Long id;
    private String name;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private int displayOrder;
    private boolean isActive;
    private List<Map<String, Object>> subgroups;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean getIsActive() { return isActive; }
    public boolean isActive() { return isActive; }
    public void setIsActive(boolean isActive) { this.isActive = isActive; }

    public List<Map<String, Object>> getSubgroups() { return subgroups; }
    public void setSubgroups(List<Map<String, Object>> subgroups) { this.subgroups = subgroups; }
}
