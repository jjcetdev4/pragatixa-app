package com.pragatix.modules.activity.dto.request;

public class EvaluatePromotionsRequest {

    private Long stageId;
    private Long academicYearId;

    public EvaluatePromotionsRequest() {
    }

    public EvaluatePromotionsRequest(Long stageId, Long academicYearId) {
        this.stageId = stageId;
        this.academicYearId = academicYearId;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }
}
