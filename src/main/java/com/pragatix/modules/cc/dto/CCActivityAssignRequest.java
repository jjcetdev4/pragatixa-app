package com.pragatix.modules.cc.dto;

import java.util.List;

public class CCActivityAssignRequest {
    private List<Long> studentIds;
    private List<String> studentRegNos;
    private String remarks;

    public CCActivityAssignRequest() {
    }

    public CCActivityAssignRequest(List<Long> studentIds, String remarks) {
        this.studentIds = studentIds;
        this.remarks = remarks;
    }

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }

    public List<String> getStudentRegNos() {
        return studentRegNos;
    }

    public void setStudentRegNos(List<String> studentRegNos) {
        this.studentRegNos = studentRegNos;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
