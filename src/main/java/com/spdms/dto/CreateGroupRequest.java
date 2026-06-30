package com.spdms.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CreateGroupRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    @Min(value = 1, message = "Group size must be at least 1")
    private int size;

    @NotBlank(message = "Captain Student ID is required")
    private String captainStudentId;

    private List<String> memberStudentIds; // other member student IDs

    public CreateGroupRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getCaptainStudentId() { return captainStudentId; }
    public void setCaptainStudentId(String captainStudentId) { this.captainStudentId = captainStudentId; }

    public List<String> getMemberStudentIds() { return memberStudentIds; }
    public void setMemberStudentIds(List<String> memberStudentIds) { this.memberStudentIds = memberStudentIds; }
}
