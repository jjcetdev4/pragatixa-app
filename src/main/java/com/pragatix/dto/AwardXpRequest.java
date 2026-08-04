package com.pragatix.dto;

import java.util.List;

public class AwardXpRequest {
    private Long regNo;
    private List<Long> studentIds;
    private Long activityId;
    private Long assignmentId;
    private int xp;
    private String remarks;
    private String result; // "PASS" or "FAIL"

    public AwardXpRequest() {
    }

    public AwardXpRequest(Long regNo, List<Long> studentIds, Long activityId, Long assignmentId, int xp, String remarks,
            String result) {
        this.regNo = regNo;
        this.studentIds = studentIds;
        this.activityId = activityId;
        this.assignmentId = assignmentId;
        this.xp = xp;
        this.remarks = remarks;
        this.result = result;
    }

    public Long getRegNo() {
        return regNo;
    }

    public void setRegNo(Long regNo) {
        this.regNo = regNo;
    }

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getResult() {
        return result != null ? result : "PASS";
    }

    public void setResult(String result) {
        this.result = result;
    }
}
