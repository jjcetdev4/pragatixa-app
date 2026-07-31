package com.pragatix.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateTeamRequest {

    @NotBlank(message = "Team name is required")
    private String name;

    @Min(value = 1, message = "Team size must be at least 1")
    private int size;

    private Long assignmentId;

    private String captainStudentId;

    private List<String> memberStudentIds; // other member student IDs

    public CreateTeamRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(Long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public String getCaptainStudentId() {
        return captainStudentId;
    }

    public void setCaptainStudentId(String captainStudentId) {
        this.captainStudentId = captainStudentId;
    }

    public List<String> getMemberStudentIds() {
        return memberStudentIds;
    }

    public void setMemberStudentIds(List<String> memberStudentIds) {
        this.memberStudentIds = memberStudentIds;
    }
}
