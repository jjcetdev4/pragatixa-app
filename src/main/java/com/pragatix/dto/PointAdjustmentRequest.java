package com.pragatix.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PointAdjustmentRequest {

    @NotNull(message = "Points adjustment value is required")
    private Integer points;

    @NotBlank(message = "Reason for adjustment is required")
    private String reason;

    private Long subgroupId; // optional/nullable activity subgroup reference

    public PointAdjustmentRequest() {
    }

    public PointAdjustmentRequest(Integer points, String reason, Long subgroupId) {
        this.points = points;
        this.reason = reason;
        this.subgroupId = subgroupId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getSubgroupId() {
        return subgroupId;
    }

    public void setSubgroupId(Long subgroupId) {
        this.subgroupId = subgroupId;
    }
}
