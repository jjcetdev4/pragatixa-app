package com.pragatix.modules.activity.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public class MyActivityResponse {
    private Long activityId;
    private String name;
    private String description;
    private String frequency;
    private List<String> evidence;
    private String xp;
    private String type;
    private String justification;
    private Long departmentId;
    private String departmentName;
    private Long sectionId;
    private String sectionName;
    private String assignedBy;
    private LocalDateTime assignedAt;
    private String assignedFacultyName;
    private String assignmentMode;
    private String xpCategory;
    private Integer awardXp;
    private Boolean awardEnabled;
    private Boolean penaltyEnabled;
    private Integer penaltyXp;
    private String awardType;
    private boolean repeatAllowed;
    private String xpType;

    // ── Refactored Award Rules ─────────────────────────────────────────────────
    private Integer cap; // max awards per frequency window
    private String awardFrequency; // One Time | Daily | Weekly | Monthly | Manual | Per Assignment
    private String awardDays; // comma-separated days, set when Weekly
    
    // ── Attendance Engine Mapping ──────────────────────────────────────────────
    private Boolean attendanceEngineEnabled;
    private String attendanceRule;
    private String manualEvidenceName;
    // ── Streak System ──────────────────────────────────────────────────────────
    private Boolean streakEnabled;

    public MyActivityResponse() {
    }

    public MyActivityResponse(Long activityId, String name, String description, String frequency,
            List<String> evidence, String xp, String type, String justification,
            Long departmentId, String departmentName, Long sectionId, String sectionName,
            String assignedBy, LocalDateTime assignedAt, String assignedFacultyName, String assignmentMode,
            String xpCategory,
            Integer awardXp, Boolean awardEnabled, Boolean penaltyEnabled, Integer penaltyXp, String awardType,
            boolean repeatAllowed, String xpType,
            Integer cap, String awardFrequency, String awardDays,
            Boolean attendanceEngineEnabled, String attendanceRule, String manualEvidenceName, Boolean streakEnabled) {
        this.activityId = activityId;
        this.name = name;
        this.description = description;
        this.frequency = frequency;
        this.evidence = evidence;
        this.xp = xp;
        this.type = type;
        this.justification = justification;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.assignedFacultyName = assignedFacultyName;
        this.assignmentMode = assignmentMode;
        this.xpCategory = xpCategory;
        this.awardXp = awardXp;
        this.awardEnabled = awardEnabled;
        this.penaltyEnabled = penaltyEnabled;
        this.penaltyXp = penaltyXp;
        this.awardType = awardType;
        this.repeatAllowed = repeatAllowed;
        this.xpType = xpType;
        this.cap = cap;
        this.awardFrequency = awardFrequency;
        this.awardDays = awardDays;
        this.attendanceEngineEnabled = attendanceEngineEnabled;
        this.attendanceRule = attendanceRule;
        this.manualEvidenceName = manualEvidenceName;
        this.streakEnabled = streakEnabled;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public List<String> getEvidence() {
        return evidence;
    }

    public void setEvidence(List<String> evidence) {
        this.evidence = evidence;
    }

    public String getXp() {
        return xp;
    }

    public void setXp(String xp) {
        this.xp = xp;
    }

    /** @deprecated use getCap() instead */
    public String getCap() {
        return cap != null ? cap.toString() : "1";
    }

    public void setCap(String cap) {
        try {
            this.cap = Integer.parseInt(cap);
        } catch (Exception ignored) {
            this.cap = 1;
        }
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public String getAssignedFacultyName() {
        return assignedFacultyName;
    }

    public void setAssignedFacultyName(String assignedFacultyName) {
        this.assignedFacultyName = assignedFacultyName;
    }

    public String getAssignmentMode() {
        return assignmentMode;
    }

    public void setAssignmentMode(String assignmentMode) {
        this.assignmentMode = assignmentMode;
    }

    public String getXpCategory() {
        return xpCategory;
    }

    public void setXpCategory(String xpCategory) {
        this.xpCategory = xpCategory;
    }

    public Integer getAwardXp() {
        return awardXp;
    }

    public void setAwardXp(Integer awardXp) {
        this.awardXp = awardXp;
    }

    public Boolean getAwardEnabled() {
        return awardEnabled;
    }

    public void setAwardEnabled(Boolean awardEnabled) {
        this.awardEnabled = awardEnabled;
    }

    public Boolean getPenaltyEnabled() {
        return penaltyEnabled;
    }

    public void setPenaltyEnabled(Boolean penaltyEnabled) {
        this.penaltyEnabled = penaltyEnabled;
    }

    public Integer getPenaltyXp() {
        return penaltyXp;
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

    public Integer getCapValue() {
        return cap;
    }

    public void setCapValue(Integer cap) {
        this.cap = cap;
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

    public Boolean getAttendanceEngineEnabled() {
        return attendanceEngineEnabled;
    }

    public void setAttendanceEngineEnabled(Boolean attendanceEngineEnabled) {
        this.attendanceEngineEnabled = attendanceEngineEnabled;
    }

    public Boolean getStreakEnabled() {
        return streakEnabled;
    }

    public void setStreakEnabled(Boolean streakEnabled) {
        this.streakEnabled = streakEnabled;
    }

    public String getAttendanceRule() {
        return attendanceRule;
    }

    public void setAttendanceRule(String attendanceRule) {
        this.attendanceRule = attendanceRule;
    }

    public String getManualEvidenceName() {
        return manualEvidenceName;
    }

    public void setManualEvidenceName(String manualEvidenceName) {
        this.manualEvidenceName = manualEvidenceName;
    }

    // ── Backward-compat getters ────────────────────────────────────────────────
    public String getXpType() {
        return xpType;
    }

    public void setXpType(String xpType) {
        this.xpType = xpType;
    }

    // ── Backward-compat getters ────────────────────────────────────────────────
    public Integer getMaximumAwards() {
        return cap;
    }

    public String getResetPeriod() {
        return awardFrequency;
    }

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
        private String type;
        private String justification;
        private Long departmentId;
        private String departmentName;
        private Long sectionId;
        private String sectionName;
        private String assignedBy;
        private LocalDateTime assignedAt;
        private String assignedFacultyName;
        private String assignmentMode;
        private String xpCategory;
        private Integer awardXp;
        private Boolean awardEnabled;
        private Boolean penaltyEnabled;
        private Integer penaltyXp;
        private String awardType;
        private boolean repeatAllowed;
        private String xpType;
        private Integer cap = 1;
        private String awardFrequency = "One Time";
        private String awardDays;

        public Builder activityId(Long v) {
            this.activityId = v;
            return this;
        }

        public Builder name(String v) {
            this.name = v;
            return this;
        }

        public Builder description(String v) {
            this.description = v;
            return this;
        }

        public Builder frequency(String v) {
            this.frequency = v;
            return this;
        }

        public Builder evidence(List<String> v) {
            this.evidence = v;
            return this;
        }

        public Builder xp(String v) {
            this.xp = v;
            return this;
        }

        public Builder type(String v) {
            this.type = v;
            return this;
        }

        public Builder justification(String v) {
            this.justification = v;
            return this;
        }

        public Builder departmentId(Long v) {
            this.departmentId = v;
            return this;
        }

        public Builder departmentName(String v) {
            this.departmentName = v;
            return this;
        }

        public Builder sectionId(Long v) {
            this.sectionId = v;
            return this;
        }

        public Builder sectionName(String v) {
            this.sectionName = v;
            return this;
        }

        public Builder assignedBy(String v) {
            this.assignedBy = v;
            return this;
        }

        public Builder assignedAt(LocalDateTime v) {
            this.assignedAt = v;
            return this;
        }

        public Builder assignedFacultyName(String v) {
            this.assignedFacultyName = v;
            return this;
        }

        public Builder assignmentMode(String v) {
            this.assignmentMode = v;
            return this;
        }

        public Builder xpCategory(String v) {
            this.xpCategory = v;
            return this;
        }

        public Builder awardXp(Integer v) {
            this.awardXp = v;
            return this;
        }

        public Builder awardEnabled(Boolean v) {
            this.awardEnabled = v;
            return this;
        }

        public Builder penaltyEnabled(Boolean v) {
            this.penaltyEnabled = v;
            return this;
        }

        public Builder penaltyXp(Integer v) {
            this.penaltyXp = v;
            return this;
        }

        public Builder awardType(String v) {
            this.awardType = v;
            return this;
        }

        public Builder repeatAllowed(boolean v) {
            this.repeatAllowed = v;
            return this;
        }

        public Builder xpType(String v) {
            this.xpType = v;
            return this;
        }

        public Builder cap(Integer v) {
            this.cap = v;
            return this;
        }

        public Builder awardFrequency(String v) {
            this.awardFrequency = v;
            return this;
        }

        public Builder awardDays(String v) {
            this.awardDays = v;
            return this;
        }

        // backward compat
        public Builder maximumAwards(Integer v) {
            this.cap = v;
            return this;
        }

        public Builder resetPeriod(String v) {
            this.awardFrequency = v;
            return this;
        }

        private Boolean attendanceEngineEnabled;
        private String attendanceRule;
        private String manualEvidenceName;
        private Boolean streakEnabled;

        public Builder attendanceEngineEnabled(Boolean v) {
            this.attendanceEngineEnabled = v;
            return this;
        }

        public Builder attendanceRule(String v) {
            this.attendanceRule = v;
            return this;
        }

        public Builder manualEvidenceName(String v) {
            this.manualEvidenceName = v;
            return this;
        }

        public Builder streakEnabled(Boolean v) {
            this.streakEnabled = v;
            return this;
        }

        public MyActivityResponse build() {
            return new MyActivityResponse(activityId, name, description, frequency, evidence,
                    xp, type, justification, departmentId, departmentName, sectionId, sectionName,
                    assignedBy, assignedAt, assignedFacultyName, assignmentMode, xpCategory, awardXp, awardEnabled,
                    penaltyEnabled, penaltyXp, awardType, repeatAllowed, xpType,
                    cap, awardFrequency, awardDays, attendanceEngineEnabled, attendanceRule, manualEvidenceName, streakEnabled);
        }
    }
}
