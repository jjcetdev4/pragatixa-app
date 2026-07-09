package com.spdms.dto;

import java.time.LocalDateTime;
import java.util.List;

public class MyActivityResponse {
    private Long activityId;
    private String name;
    private String description;
    private String frequency;
    private List<String> evidence;
    private String xp;
    private String cap;
    private String type;
    private String justification;
    private Long departmentId;
    private String departmentName;
    private Long sectionId;
    private String sectionName;
    private String assignedBy;
    private LocalDateTime assignedAt;

    public MyActivityResponse() {}

    public MyActivityResponse(Long activityId, String name, String description, String frequency, List<String> evidence, String xp, String cap, String type, String justification, Long departmentId, String departmentName, Long sectionId, String sectionName, String assignedBy, LocalDateTime assignedAt) {
        this.activityId = activityId;
        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.evidence = evidence;
        this.xp = xp;
        this.cap = cap;
        this.type = type;
        this.justification = justification;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public List<String> getEvidence() { return evidence; }
    public void setEvidence(List<String> evidence) { this.evidence = evidence; }

    public String getXp() { return xp; }
    public void setXp(String xp) { this.xp = xp; }

    public String getCap() { return cap; }
    public void setCap(String cap) { this.cap = cap; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long activityId;
        private String name;
        private String description;
        private String frequency;
        private List<String> evidence;
        private String xp;
        private String cap;
        private String type;
        private String justification;
        private Long departmentId;
        private String departmentName;
        private Long sectionId;
        private String sectionName;
        private String assignedBy;
        private LocalDateTime assignedAt;

        public Builder activityId(Long activityId) { this.activityId = activityId; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder frequency(String frequency) { this.frequency = frequency; return this; }
        public Builder evidence(List<String> evidence) { this.evidence = evidence; return this; }
        public Builder xp(String xp) { this.xp = xp; return this; }
        public Builder cap(String cap) { this.cap = cap; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder justification(String justification) { this.justification = justification; return this; }
        public Builder departmentId(Long departmentId) { this.departmentId = departmentId; return this; }
        public Builder departmentName(String departmentName) { this.departmentName = departmentName; return this; }
        public Builder sectionId(Long sectionId) { this.sectionId = sectionId; return this; }
        public Builder sectionName(String sectionName) { this.sectionName = sectionName; return this; }
        public Builder assignedBy(String assignedBy) { this.assignedBy = assignedBy; return this; }
        public Builder assignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; return this; }

        public MyActivityResponse build() {
            return new MyActivityResponse(activityId, name, description, frequency, evidence, xp, cap, type, justification, departmentId, departmentName, sectionId, sectionName, assignedBy, assignedAt);
        }
    }
}
