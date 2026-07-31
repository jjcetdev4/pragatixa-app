package com.pragatix.dto;

import java.time.LocalDateTime;

public class XpTransactionDto {
    private Long id;
    private String studentRegNo;
    private Long activityId;
    private String category;
    private String activityName;
    private int xpPoints;
    private String evidenceUrl;
    private LocalDateTime submittedAt;
    private String status;
    private String approvedBy;
    private boolean isPenalty;
    private boolean capApplied;

    public XpTransactionDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentRegNo() {
        return studentRegNo;
    }

    public void setStudentRegNo(String studentRegNo) {
        this.studentRegNo = studentRegNo;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public int getXpPoints() {
        return xpPoints;
    }

    public void setXpPoints(int xpPoints) {
        this.xpPoints = xpPoints;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public boolean isPenalty() {
        return isPenalty;
    }

    public void setPenalty(boolean penalty) {
        isPenalty = penalty;
    }

    public boolean isCapApplied() {
        return capApplied;
    }

    public void setCapApplied(boolean capApplied) {
        this.capApplied = capApplied;
    }
}
