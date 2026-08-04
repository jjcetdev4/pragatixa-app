package com.pragatix.modules.activity.dto.response;

import java.util.List;
import com.pragatix.modules.activity.dto.response.ActivityResponse;

public class ActivitySubgroupResponse {
    private Long id;
    private String name;
    @Deprecated
    private Integer threshold;
    private Long assignedFacultyId;
    private String assignedFacultyName;
    private List<ActivityResponse> activities;

    public ActivitySubgroupResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Deprecated
    public Integer getThreshold() {
        return threshold;
    }

    @Deprecated
    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }

    public Long getAssignedFacultyId() {
        return assignedFacultyId;
    }

    public void setAssignedFacultyId(Long assignedFacultyId) {
        this.assignedFacultyId = assignedFacultyId;
    }

    public String getAssignedFacultyName() {
        return assignedFacultyName;
    }

    public void setAssignedFacultyName(String assignedFacultyName) {
        this.assignedFacultyName = assignedFacultyName;
    }

    public List<ActivityResponse> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityResponse> activities) {
        this.activities = activities;
    }
}
