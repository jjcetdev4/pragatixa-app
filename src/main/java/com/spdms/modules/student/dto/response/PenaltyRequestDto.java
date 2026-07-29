package com.pragatix.modules.student.dto.response;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

public class PenaltyRequestDto {
    private Long id;
    private String studentName;
    private String regNo;
    private String department;
    private String year;
    private String section;
    private String penaltyActivity;

    @JsonProperty("penaltyXP")
    private int penaltyXP;

    private String reason;
    private String submittedBy;
    private LocalDateTime submittedTime;
    private String status;
    private String approvedBy;
    private LocalDateTime approvalTime;
    private String rejectedReason;

    public PenaltyRequestDto() {
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getPenaltyActivity() {
        return penaltyActivity;
    }

    public void setPenaltyActivity(String penaltyActivity) {
        this.penaltyActivity = penaltyActivity;
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

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        this.submittedBy = submittedBy;
    }

    public LocalDateTime getSubmittedTime() {
        return submittedTime;
    }

    public void setSubmittedTime(LocalDateTime submittedTime) {
        this.submittedTime = submittedTime;
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

    public LocalDateTime getApprovalTime() {
        return approvalTime;
    }

    public void setApprovalTime(LocalDateTime approvalTime) {
        this.approvalTime = approvalTime;
    }

    public String getRejectedReason() {
        return rejectedReason;
    }

    public void setRejectedReason(String rejectedReason) {
        this.rejectedReason = rejectedReason;
    }
}
