package com.pragatix.modules.student.dto.request;

public class CreatePenaltyRequestDto {
    private String regNo;
    private Long activityId;
    private String activityName;
    private int penaltyXP;
    private String reason;

    public CreatePenaltyRequestDto() {
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getPenaltyXP() {
        return penaltyXP;
    }

    public void setPenaltyXP(int penaltyXP) {
        this.penaltyXP = penaltyXP;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
