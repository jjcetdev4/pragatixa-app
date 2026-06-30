package com.spdms.dto;

import java.util.List;

public class GroupResponse {
    private Long id;
    private String name;
    private int size;
    private String captainId;
    private String captainName;
    private List<StudentResponse> members;

    public GroupResponse() {}

    public GroupResponse(Long id, String name, int size, String captainId, String captainName, List<StudentResponse> members) {
        this.id = id;
        this.name = name;
        this.size = size;
        this.captainId = captainId;
        this.captainName = captainName;
        this.members = members;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }

    public String getCaptainId() { return captainId; }
    public void setCaptainId(String captainId) { this.captainId = captainId; }

    public String getCaptainName() { return captainName; }
    public void setCaptainName(String captainName) { this.captainName = captainName; }

    public List<StudentResponse> getMembers() { return members; }
    public void setMembers(List<StudentResponse> members) { this.members = members; }
}
