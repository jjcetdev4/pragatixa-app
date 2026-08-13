package jjcet.PragatiX.modules.cc.dto;

public class CCTeacherAssignRequest {
    private Long teacherId;
    private Long stageId;
    private String assignmentDuration; // "ONLY_TODAY" or "PERMANENT"
    private String remarks;

    public CCTeacherAssignRequest() {
    }

    public CCTeacherAssignRequest(Long teacherId, Long stageId, String assignmentDuration, String remarks) {
        this.teacherId = teacherId;
        this.stageId = stageId;
        this.assignmentDuration = assignmentDuration;
        this.remarks = remarks;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public String getAssignmentDuration() {
        return assignmentDuration;
    }

    public void setAssignmentDuration(String assignmentDuration) {
        this.assignmentDuration = assignmentDuration;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
