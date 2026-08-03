package com.pragatix.modules.analytics.dto;

public class GroupedAttendanceDTO {
    private String label;
    private Double attendancePercentage;

    public GroupedAttendanceDTO() {
    }

    public GroupedAttendanceDTO(String label, Double attendancePercentage) {
        this.label = label;
        this.attendancePercentage = attendancePercentage;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}
