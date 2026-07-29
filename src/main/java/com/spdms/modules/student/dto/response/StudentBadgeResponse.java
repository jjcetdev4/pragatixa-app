package com.pragatix.modules.student.dto.response;

import com.pragatix.entity.StudentBadge;
import java.time.LocalDateTime;

public class StudentBadgeResponse {
    private Long id;
    private String regNo;
    private String studentName;
    private Long badgeId;
    private String badgeName;
    private String description;
    private String tier;
    private String iconUrl;
    private String status;
    private String approvalAuthority;
    private String rarity;
    private String evidenceUrl;
    private LocalDateTime awardedAt;

    public StudentBadgeResponse() {
    }

    public StudentBadgeResponse(StudentBadge claim) {
        this.id = claim.getId();
        this.status = claim.getStatus();
        this.evidenceUrl = claim.getEvidenceUrl();
        this.awardedAt = claim.getAwardedAt();

        if (claim.getStudent() != null) {
            this.regNo = claim.getStudent().getRegNo();
            this.studentName = claim.getStudent().getFullName();
        }

        if (claim.getBadge() != null) {
            this.badgeId = claim.getBadge().getId();
            this.badgeName = claim.getBadge().getName();
            this.description = claim.getBadge().getDescription();
            this.tier = claim.getBadge().getTier();
            this.iconUrl = claim.getBadge().getIconUrl();
            this.approvalAuthority = claim.getBadge().getApprovalAuthority();
            this.rarity = claim.getBadge().getRarity();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Long getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(Long badgeId) {
        this.badgeId = badgeId;
    }

    public String getBadgeName() {
        return badgeName;
    }

    public void setBadgeName(String badgeName) {
        this.badgeName = badgeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getApprovalAuthority() {
        return approvalAuthority;
    }

    public void setApprovalAuthority(String approvalAuthority) {
        this.approvalAuthority = approvalAuthority;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public LocalDateTime getAwardedAt() {
        return awardedAt;
    }

    public void setAwardedAt(LocalDateTime awardedAt) {
        this.awardedAt = awardedAt;
    }
}
