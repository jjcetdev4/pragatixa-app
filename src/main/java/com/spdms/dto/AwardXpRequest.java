package com.spdms.dto;

public class AwardXpRequest {
    private Long studentId;
    private Long activityId;
    private Long assignmentId;
    private int xp;
    private String remarks;

    public AwardXpRequest() {}

    public AwardXpRequest(Long studentId, Long activityId, Long assignmentId, int xp, String remarks) {
        this.studentId = studentId;
        this.activityId = activityId;
        this.assignmentId = assignmentId;
        this.xp = xp;
        this.remarks = remarks;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public int getXp() { return xp; }
    public void setXp(int xp) { this.xp = xp; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
}
