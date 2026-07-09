package com.spdms.dto;

import java.time.LocalDateTime;

public class ActivityAssignmentResponse {
    private Long id;
    private Long activityId;
    private String activityName;
    private Long departmentId;
    private String departmentName;
    private Long sectionId;
    private String sectionName;
    private Long teacherId;
    private String teacherName;
    private String teacherUsername;
    private String assignedBy;
    private LocalDateTime assignedAt;

    public ActivityAssignmentResponse() {}

    public ActivityAssignmentResponse(Long id, Long activityId, String activityName, Long departmentId, String departmentName, Long sectionId, String sectionName, Long teacherId, String teacherName, String teacherUsername, String assignedBy, LocalDateTime assignedAt) {
        this.id = id;
        this.activityId = activityId;
        this.activityName = activityName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.teacherUsername = teacherUsername;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public Long getSectionId() { return sectionId; }
    public void setSectionId(Long sectionId) { this.sectionId = sectionId; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTeacherUsername() { return teacherUsername; }
    public void setTeacherUsername(String teacherUsername) { this.teacherUsername = teacherUsername; }

    public String getAssignedBy() { return assignedBy; }
    public void setAssignedBy(String assignedBy) { this.assignedBy = assignedBy; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long activityId;
        private String activityName;
        private Long departmentId;
        private String departmentName;
        private Long sectionId;
        private String sectionName;
        private Long teacherId;
        private String teacherName;
        private String teacherUsername;
        private String assignedBy;
        private LocalDateTime assignedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder activityId(Long activityId) { this.activityId = activityId; return this; }
        public Builder activityName(String activityName) { this.activityName = activityName; return this; }
        public Builder departmentId(Long departmentId) { this.departmentId = departmentId; return this; }
        public Builder departmentName(String departmentName) { this.departmentName = departmentName; return this; }
        public Builder sectionId(Long sectionId) { this.sectionId = sectionId; return this; }
        public Builder sectionName(String sectionName) { this.sectionName = sectionName; return this; }
        public Builder teacherId(Long teacherId) { this.teacherId = teacherId; return this; }
        public Builder teacherName(String teacherName) { this.teacherName = teacherName; return this; }
        public Builder teacherUsername(String teacherUsername) { this.teacherUsername = teacherUsername; return this; }
        public Builder assignedBy(String assignedBy) { this.assignedBy = assignedBy; return this; }
        public Builder assignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; return this; }

        public ActivityAssignmentResponse build() {
            return new ActivityAssignmentResponse(id, activityId, activityName, departmentId, departmentName, sectionId, sectionName, teacherId, teacherName, teacherUsername, assignedBy, assignedAt);
        }
    }
}
