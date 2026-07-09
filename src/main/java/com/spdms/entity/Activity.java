package com.spdms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "activities", uniqueConstraints = {
    @UniqueConstraint(name = "uq_activity", columnNames = {"category_id", "activity_name"})
})
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private ActivityCategory activityCategory;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stage_id")
    private ActivityStage stage;

    @Column(name = "activity_name", nullable = false, length = 255)
    private String activityName;

    @Column(name = "activity_description", columnDefinition = "TEXT")
    private String activityDescription;

    @Column(name = "mode_type", nullable = false, length = 50)
    private String modeType;

    @Column(length = 100)
    private String frequency;

    @Column(name = "max_points", nullable = false)
    private int maxPoints;

    @Column(length = 100)
    private String xp;

    @Column(length = 100)
    private String cap;

    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory;

    @Column(name = "evidence_required", nullable = false)
    private boolean evidenceRequired = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 255)
    private String evidence;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "owner_department", length = 100)
    private String ownerDepartment;

    @Column(name = "owner_subrole", length = 100)
    private String ownerSubrole;

    @Column(length = 50)
    private String type;

    @Column(name = "xp_category", length = 100)
    private String xpCategory;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subgroup_id", nullable = false)
    private ActivitySubgroup subgroup;

    @Transient
    private String departmentId;

    @Transient
    private String teacherId;

    @Transient
    private List<Map<String, Object>> assignmentSummary;

    public Activity() {}

    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public List<Map<String, Object>> getAssignmentSummary() { return assignmentSummary; }
    public void setAssignmentSummary(List<Map<String, Object>> assignmentSummary) { this.assignmentSummary = assignmentSummary; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public ActivityCategory getActivityCategory() { return activityCategory; }
    public void setActivityCategory(ActivityCategory activityCategory) { this.activityCategory = activityCategory; }

    public ActivityStage getStage() { return stage; }
    public void setStage(ActivityStage stage) { this.stage = stage; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getActivityDescription() { return activityDescription; }
    public void setActivityDescription(String activityDescription) { this.activityDescription = activityDescription; }

    public String getModeType() { return modeType; }
    public void setModeType(String modeType) { this.modeType = modeType; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public int getMaxPoints() { return maxPoints; }
    public void setMaxPoints(int maxPoints) { this.maxPoints = maxPoints; }

    public String getXp() { return xp; }
    public void setXp(String xp) { this.xp = xp; }

    public String getCap() { return cap; }
    public void setCap(String cap) { this.cap = cap; }

    public boolean isMandatory() { return isMandatory; }
    public void setMandatory(boolean mandatory) { isMandatory = mandatory; }

    public boolean isEvidenceRequired() { return evidenceRequired; }
    public void setEvidenceRequired(boolean evidenceRequired) { this.evidenceRequired = evidenceRequired; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public String getJustification() { return justification; }
    public void setJustification(String justification) { this.justification = justification; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerDepartment() { return ownerDepartment; }
    public void setOwnerDepartment(String ownerDepartment) { this.ownerDepartment = ownerDepartment; }

    public String getOwnerSubrole() { return ownerSubrole; }
    public void setOwnerSubrole(String ownerSubrole) { this.ownerSubrole = ownerSubrole; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getXpCategory() { return xpCategory; }
    public void setXpCategory(String xpCategory) { this.xpCategory = xpCategory; }

    public ActivitySubgroup getSubgroup() { return subgroup; }
    public void setSubgroup(ActivitySubgroup subgroup) { this.subgroup = subgroup; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final Activity a = new Activity();
        public Builder activityCategory(ActivityCategory v) { a.activityCategory = v; return this; }
        public Builder stage(ActivityStage v) { a.stage = v; return this; }
        public Builder activityName(String v) { a.activityName = v; return this; }
        public Builder activityDescription(String v) { a.activityDescription = v; return this; }
        public Builder modeType(String v) { a.modeType = v; return this; }
        public Builder frequency(String v) { a.frequency = v; return this; }
        public Builder maxPoints(int v) { a.maxPoints = v; return this; }
        public Builder xp(String v) { a.xp = v; return this; }
        public Builder cap(String v) { a.cap = v; return this; }
        public Builder isMandatory(boolean v) { a.isMandatory = v; return this; }
        public Builder evidenceRequired(boolean v) { a.evidenceRequired = v; return this; }
        public Builder category(String v) { a.category = v; return this; }
        public Builder description(String v) { a.description = v; return this; }
        public Builder evidence(String v) { a.evidence = v; return this; }
        public Builder justification(String v) { a.justification = v; return this; }
        public Builder name(String v) { a.name = v; return this; }
        public Builder ownerDepartment(String v) { a.ownerDepartment = v; return this; }
        public Builder ownerSubrole(String v) { a.ownerSubrole = v; return this; }
        public Builder type(String v) { a.type = v; return this; }
        public Builder xpCategory(String v) { a.xpCategory = v; return this; }
        public Builder subgroup(ActivitySubgroup v) { a.subgroup = v; return this; }
        public Activity build() { return a; }
    }
}
