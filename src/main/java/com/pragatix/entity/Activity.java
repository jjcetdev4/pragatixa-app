package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "activities", uniqueConstraints = {
        @UniqueConstraint(name = "uq_activity_subgroup", columnNames = { "subgroup_id", "activity_name" })
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

    @Column(name = "max_points", nullable = false)
    private int maxPoints;

    @Column(name = "award_xp", nullable = false)
    private Integer awardXp = 0;

    @Column(name = "award_enabled", nullable = false)
    private Boolean awardEnabled = true;

    @Column(name = "penalty_enabled", nullable = false)
    private Boolean penaltyEnabled = false;

    @Column(name = "penalty_xp", nullable = false)
    private Integer penaltyXp = 0;

    @Column(name = "award_type", nullable = false, length = 50)
    private String awardType = "Fixed XP";

    @Column(name = "repeat_allowed", nullable = false)
    private boolean repeatAllowed = false;

    @Column(name = "reset_period", length = 50)
    private String resetPeriod = "Once";

    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory;

    @Column(name = "evidence_required", nullable = false)
    private boolean evidenceRequired = true;

    @Column(name = "allow_student_request")
    private Boolean allowStudentRequest = false;

    @Column(name = "streak_enabled", nullable = false)
    private Boolean streakEnabled = false;

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

    @Column(name = "manual_evidence_name", length = 255)
    private String manualEvidenceName;

    @Column(columnDefinition = "TEXT")
    private String justification;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "owner_department", length = 100)
    private String ownerDepartment;

    @Column(name = "owner_subrole", length = 100)
    private String ownerSubrole;

    @Column(length = 50)
    private String type;

    @Column(name = "xp_category", length = 100)
    private String xpCategory;

    @Column(name = "xp_type", length = 50)
    private String xpType = "Reward";

    @Column(name = "maximum_awards")
    private Integer maximumAwards = 1;

    @Column(name = "award_frequency", length = 50)
    private String awardFrequency = "One Time";

    @Column(name = "award_days", length = 200)
    private String awardDays;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "assignment_mode", length = 50)
    private String assignmentMode = "MANUAL";

    /**
     * When true, this activity is managed exclusively by the Attendance Engine.
     * Manual XP awards (Teacher, CC, Admin) must be blocked for this activity.
     * Only one activity per Academic Year may have this set to true.
     */
    @Column(name = "attendance_engine_enabled", nullable = false)
    private Boolean attendanceEngineEnabled = false;

    /**
     * Determines which engine phase uses this activity for XP.
     * Values: DAILY | WEEKLY | BOTH
     */
    @Column(name = "attendance_rule", length = 20)
    private String attendanceRule;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_year")
    private com.pragatix.enums.AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subgroup_id", nullable = false)
    private ActivitySubgroup subgroup;

    @Transient
    private String departmentId;

    @Transient
    private String teacherId;

    @Transient
    private List<Map<String, Object>> assignmentSummary;

    public Activity() {
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public List<Map<String, Object>> getAssignmentSummary() {
        return assignmentSummary;
    }

    public void setAssignmentSummary(List<Map<String, Object>> assignmentSummary) {
        this.assignmentSummary = assignmentSummary;
    }
    
    // XP Eligibility Helpers for Promotion Validation Engine
    public boolean isMustXpEligible() {
        return "Reward".equalsIgnoreCase(this.xpType) && this.subgroup != null && "Must".equalsIgnoreCase(this.subgroup.getName());
    }
    
    public boolean isIndividualXpEligible() {
        return "Reward".equalsIgnoreCase(this.xpType) && "Individual".equalsIgnoreCase(this.modeType);
    }
    
    public boolean isGroupXpEligible() {
        return "Reward".equalsIgnoreCase(this.xpType) && "Group".equalsIgnoreCase(this.modeType);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActivityCategory getActivityCategory() {
        return activityCategory;
    }

    public void setActivityCategory(ActivityCategory activityCategory) {
        this.activityCategory = activityCategory;
    }

    public ActivityStage getStage() {
        return stage;
    }

    public void setStage(ActivityStage stage) {
        this.stage = stage;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public void setActivityDescription(String activityDescription) {
        this.activityDescription = activityDescription;
    }

    public String getModeType() {
        return modeType;
    }

    public void setModeType(String modeType) {
        this.modeType = modeType;
    }

    public String getFrequency() {
        return resetPeriod != null ? resetPeriod : "Once";
    }

    public void setFrequency(String frequency) {
        this.resetPeriod = frequency;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public void setMaxPoints(int maxPoints) {
        this.maxPoints = maxPoints;
    }

    public String getXp() {
        if ("Penalty".equalsIgnoreCase(xpType)) {
            return penaltyXp != null ? "-" + penaltyXp : "0";
        }
        return awardXp != null ? awardXp.toString() : "0";
    }

    public void setXp(String xp) {
        try {
            this.awardXp = Integer.parseInt(xp);
        } catch (Exception ignored) {
        }
    }

    public Integer getCap() {
        if ("Per Assignment".equalsIgnoreCase(awardFrequency))
            return null;
        return maximumAwards != null ? maximumAwards : 1;
    }

    public void setCap(Object cap) {
        if ("Per Assignment".equalsIgnoreCase(awardFrequency)) {
            this.maximumAwards = null;
            return;
        }
        if (cap == null) {
            this.maximumAwards = 1;
            return;
        }
        try {
            this.maximumAwards = cap instanceof Number ? ((Number) cap).intValue() : Integer.parseInt(cap.toString());
        } catch (Exception ignored) {
            this.maximumAwards = 1;
        }
    }

    public Integer getAwardXp() {
        return awardXp;
    }

    public void setAwardXp(Integer awardXp) {
        this.awardXp = awardXp;
    }

    public Boolean getAwardEnabled() {
        return awardEnabled != null ? awardEnabled : false;
    }

    public void setAwardEnabled(Boolean awardEnabled) {
        this.awardEnabled = awardEnabled;
    }

    public Boolean getPenaltyEnabled() {
        return penaltyEnabled != null ? penaltyEnabled : false;
    }

    public void setPenaltyEnabled(Boolean penaltyEnabled) {
        this.penaltyEnabled = penaltyEnabled;
    }

    public Boolean getAllowStudentRequest() {
        return allowStudentRequest;
    }

    public void setAllowStudentRequest(Boolean allowStudentRequest) {
        this.allowStudentRequest = allowStudentRequest;
    }

    public Boolean getStreakEnabled() {
        return streakEnabled != null ? streakEnabled : false;
    }

    public void setStreakEnabled(Boolean streakEnabled) {
        this.streakEnabled = streakEnabled;
    }

    public Integer getPenaltyXp() {
        return penaltyXp != null ? penaltyXp : 0;
    }

    public void setPenaltyXp(Integer penaltyXp) {
        this.penaltyXp = penaltyXp;
    }

    public String getAwardType() {
        return awardType;
    }

    public void setAwardType(String awardType) {
        this.awardType = awardType;
    }

    public boolean isRepeatAllowed() {
        return repeatAllowed;
    }

    public void setRepeatAllowed(boolean repeatAllowed) {
        this.repeatAllowed = repeatAllowed;
    }

    public String getResetPeriod() {
        return resetPeriod;
    }

    public void setResetPeriod(String resetPeriod) {
        this.resetPeriod = resetPeriod;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public boolean isEvidenceRequired() {
        return evidenceRequired;
    }

    public void setEvidenceRequired(boolean evidenceRequired) {
        this.evidenceRequired = evidenceRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getManualEvidenceName() {
        return manualEvidenceName;
    }

    public void setManualEvidenceName(String manualEvidenceName) {
        this.manualEvidenceName = manualEvidenceName;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwnerDepartment() {
        return ownerDepartment;
    }

    public void setOwnerDepartment(String ownerDepartment) {
        this.ownerDepartment = ownerDepartment;
    }

    public String getOwnerSubrole() {
        return ownerSubrole;
    }

    public void setOwnerSubrole(String ownerSubrole) {
        this.ownerSubrole = ownerSubrole;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getXpCategory() {
        return xpCategory;
    }

    public void setXpCategory(String xpCategory) {
        this.xpCategory = xpCategory;
    }

    public String getXpType() {
        return xpType != null ? xpType : "Reward";
    }

    public void setXpType(String xpType) {
        this.xpType = xpType;
    }

    public ActivitySubgroup getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(ActivitySubgroup subgroup) {
        this.subgroup = subgroup;
    }

    public Integer getMaximumAwards() {
        return maximumAwards;
    }

    public void setMaximumAwards(Integer maximumAwards) {
        this.maximumAwards = maximumAwards;
    }

    public String getAwardFrequency() {
        return awardFrequency != null ? awardFrequency : "One Time";
    }

    public void setAwardFrequency(String awardFrequency) {
        this.awardFrequency = awardFrequency;
    }

    public String getAwardDays() {
        return awardDays;
    }

    public void setAwardDays(String awardDays) {
        this.awardDays = awardDays;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getAttendanceEngineEnabled() {
        return attendanceEngineEnabled != null ? attendanceEngineEnabled : false;
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

    public String getAssignmentMode() {
        return assignmentMode != null ? assignmentMode : "MANUAL";
    }

    public void setAssignmentMode(String assignmentMode) {
        this.assignmentMode = assignmentMode;
    }

    public com.pragatix.enums.AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(com.pragatix.enums.AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Activity a = new Activity();

        public Builder activityCategory(ActivityCategory v) {
            a.activityCategory = v;
            return this;
        }

        public Builder stage(ActivityStage v) {
            a.stage = v;
            return this;
        }

        public Builder activityName(String v) {
            a.activityName = v;
            return this;
        }

        public Builder activityDescription(String v) {
            a.activityDescription = v;
            return this;
        }

        public Builder modeType(String v) {
            a.modeType = v;
            return this;
        }

        public Builder frequency(String v) {
            a.setFrequency(v);
            return this;
        }

        public Builder maxPoints(int v) {
            a.maxPoints = v;
            return this;
        }

        public Builder xp(String v) {
            a.setXp(v);
            return this;
        }

        public Builder cap(String v) {
            a.setCap(v);
            return this;
        }

        public Builder awardXp(Integer v) {
            a.awardXp = v;
            return this;
        }

        public Builder awardEnabled(Boolean v) {
            a.awardEnabled = v;
            return this;
        }

        public Builder penaltyEnabled(Boolean v) {
            a.penaltyEnabled = v;
            return this;
        }

        public Builder penaltyXp(Integer v) {
            a.penaltyXp = v;
            return this;
        }

        public Builder awardType(String v) {
            a.awardType = v;
            return this;
        }

        public Builder repeatAllowed(boolean v) {
            a.repeatAllowed = v;
            return this;
        }

        public Builder resetPeriod(String v) {
            a.resetPeriod = v;
            return this;
        }

        public Builder isMandatory(boolean v) {
            a.isMandatory = v;
            return this;
        }

        public Builder evidenceRequired(boolean v) {
            a.evidenceRequired = v;
            return this;
        }

        public Builder category(String v) {
            a.category = v;
            return this;
        }

        public Builder description(String v) {
            a.description = v;
            return this;
        }

        public Builder evidence(String v) {
            a.evidence = v;
            return this;
        }

        public Builder justification(String v) {
            a.justification = v;
            return this;
        }

        public Builder name(String v) {
            a.name = v;
            return this;
        }

        public Builder ownerDepartment(String v) {
            a.ownerDepartment = v;
            return this;
        }

        public Builder ownerSubrole(String v) {
            a.ownerSubrole = v;
            return this;
        }

        public Builder type(String v) {
            a.type = v;
            return this;
        }

        public Builder xpCategory(String v) {
            a.xpCategory = v;
            return this;
        }

        public Builder xpType(String v) {
            a.xpType = v;
            return this;
        }

        public Builder subgroup(ActivitySubgroup v) {
            a.subgroup = v;
            return this;
        }

        public Builder maximumAwards(Integer v) {
            a.maximumAwards = v;
            return this;
        }

        public Builder awardFrequency(String v) {
            a.awardFrequency = v;
            return this;
        }

        public Builder awardDays(String v) {
            a.awardDays = v;
            return this;
        }

        public Builder displayOrder(int v) {
            a.displayOrder = v;
            return this;
        }

        public Builder status(String v) {
            a.status = v;
            return this;
        }

        public Builder assignmentMode(String v) {
            a.assignmentMode = v;
            return this;
        }

        public Builder academicYear(com.pragatix.enums.AcademicYear v) {
            a.academicYear = v;
            return this;
        }

        public Activity build() {
            return a;
        }
    }
}
