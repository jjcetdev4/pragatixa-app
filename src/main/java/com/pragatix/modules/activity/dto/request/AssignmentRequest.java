package com.pragatix.modules.activity.dto.request;

import jakarta.validation.constraints.NotNull;
import com.pragatix.entity.AssignmentScope;

public class AssignmentRequest {

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    private Long sectionId;

    private Long teacherId;

    private String year;

    @NotNull(message = "Assignment Scope is required")
    private AssignmentScope scope;

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public AssignmentScope getScope() {
        return scope;
    }

    public void setScope(AssignmentScope scope) {
        this.scope = scope;
    }
}
