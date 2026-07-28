package com.spdms.modules.activity.dto.response;

public class ActivityOptionDTO {
    private Long id;
    private String name;
    private String description;
    private Integer awardXp;
    private String awardFrequency;
    private String type;
    private String academicYear;
    private String status;
    private String category;
    private String subgroup;
    private String activityType;
    private Boolean alreadyMapped;
    private Long stageId;

    public ActivityOptionDTO() {}

    public ActivityOptionDTO(Long id, String name, String description, Integer awardXp, String awardFrequency, String type, 
                             String academicYear, String status, String category, String subgroup, String activityType, Boolean alreadyMapped, Long stageId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.awardXp = awardXp;
        this.awardFrequency = awardFrequency;
        this.type = type;
        this.academicYear = academicYear;
        this.status = status;
        this.category = category;
        this.subgroup = subgroup;
        this.activityType = activityType;
        this.alreadyMapped = alreadyMapped;
        this.stageId = stageId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getAwardXp() {
        return awardXp;
    }

    public void setAwardXp(Integer awardXp) {
        this.awardXp = awardXp;
    }

    public String getAwardFrequency() {
        return awardFrequency;
    }

    public void setAwardFrequency(String awardFrequency) {
        this.awardFrequency = awardFrequency;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(String subgroup) {
        this.subgroup = subgroup;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Boolean getAlreadyMapped() {
        return alreadyMapped;
    }

    public void setAlreadyMapped(Boolean alreadyMapped) {
        this.alreadyMapped = alreadyMapped;
    }

    public Long getStageId() {
        return stageId;
    }

    public void setStageId(Long stageId) {
        this.stageId = stageId;
    }
}
