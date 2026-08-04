package com.pragatix.modules.activity.dto.response;

public class ActivityResponse {
    private Long activityId;
    private String activityName;
    private String description;
    private Integer rewardXp;
    private String facultyName;
    private Long facultyId;
    private String frequency;
    private String evidence;
    private String status;
    private Integer awardedXp;
    private Integer requiredXp;
    private Integer remainingXp;
    private Boolean completed;
    private Boolean allowStudentRequest;
    private Boolean attendanceEngineEnabled;
    private String attendanceRule;

    public ActivityResponse() {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getRewardXp() {
        return rewardXp;
    }

    public void setRewardXp(Integer rewardXp) {
        this.rewardXp = rewardXp;
    }

    public String getFacultyName() {
        return facultyName;
    }

    public void setFacultyName(String facultyName) {
        this.facultyName = facultyName;
    }

    public Long getFacultyId() {
        return facultyId;
    }

    public void setFacultyId(Long facultyId) {
        this.facultyId = facultyId;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAwardedXp() {
        return awardedXp;
    }

    public void setAwardedXp(Integer awardedXp) {
        this.awardedXp = awardedXp;
    }

    public Integer getRequiredXp() {
        return requiredXp;
    }

    public void setRequiredXp(Integer requiredXp) {
        this.requiredXp = requiredXp;
    }

    public Integer getRemainingXp() {
        return remainingXp;
    }

    public void setRemainingXp(Integer remainingXp) {
        this.remainingXp = remainingXp;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getAllowStudentRequest() {
        return allowStudentRequest;
    }

    public void setAllowStudentRequest(Boolean allowStudentRequest) {
        this.allowStudentRequest = allowStudentRequest;
    }

    public Boolean getAttendanceEngineEnabled() {
        return attendanceEngineEnabled;
    }

    public void setAttendanceEngineEnabled(Boolean attendanceEngineEnabled) {
        this.attendanceEngineEnabled = attendanceEngineEnabled;
    }

    public String getAttendanceRule() {
        return attendanceRule;
    }

    public void setAttendanceRule(String attendanceRule) {
        this.attendanceRule = attendanceRule;
    }

    private String manualEvidenceName;

    public String getManualEvidenceName() {
        return manualEvidenceName;
    }

    public void setManualEvidenceName(String manualEvidenceName) {
        this.manualEvidenceName = manualEvidenceName;
    }
}
