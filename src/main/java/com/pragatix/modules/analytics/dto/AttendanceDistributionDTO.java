package com.pragatix.modules.analytics.dto;

public class AttendanceDistributionDTO {
    private Double presentPercentage;
    private Double partialAbsentPercentage;
    private Double fullAbsentPercentage;

    public AttendanceDistributionDTO() {
    }

    public AttendanceDistributionDTO(Double presentPercentage, Double partialAbsentPercentage, Double fullAbsentPercentage) {
        this.presentPercentage = presentPercentage;
        this.partialAbsentPercentage = partialAbsentPercentage;
        this.fullAbsentPercentage = fullAbsentPercentage;
    }

    public Double getPresentPercentage() {
        return presentPercentage;
    }

    public void setPresentPercentage(Double presentPercentage) {
        this.presentPercentage = presentPercentage;
    }

    public Double getPartialAbsentPercentage() {
        return partialAbsentPercentage;
    }

    public void setPartialAbsentPercentage(Double partialAbsentPercentage) {
        this.partialAbsentPercentage = partialAbsentPercentage;
    }

    public Double getFullAbsentPercentage() {
        return fullAbsentPercentage;
    }

    public void setFullAbsentPercentage(Double fullAbsentPercentage) {
        this.fullAbsentPercentage = fullAbsentPercentage;
    }
}
