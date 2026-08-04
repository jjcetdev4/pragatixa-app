package com.pragatix.modules.activity.dto.response;

import java.util.List;

public class GroupedActivityResponse {
    private String subgroup;
    private List<ActivityOptionDTO> activities;

    public GroupedActivityResponse() {
    }

    public GroupedActivityResponse(String subgroup, List<ActivityOptionDTO> activities) {
        this.subgroup = subgroup;
        this.activities = activities;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    public List<ActivityOptionDTO> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityOptionDTO> activities) {
        this.activities = activities;
    }
}
