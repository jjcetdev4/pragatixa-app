package com.pragatix.modules.activity.dto.response;

public class ActivityOptionDTO {
    private Long id;
    private String name;
    private String description;
    private Integer awardXp;
    private String awardFrequency;
    private String type;
    private boolean alreadyMapped;

    public ActivityOptionDTO() {
    }

    public ActivityOptionDTO(Long id, String name, String description, Integer awardXp, String awardFrequency,
            String type) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.awardXp = awardXp;
        this.awardFrequency = awardFrequency;
        this.type = type;
        this.alreadyMapped = false;
    }

    public ActivityOptionDTO(Long id, String name, String description, Integer awardXp, String awardFrequency,
            String type, boolean alreadyMapped) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.awardXp = awardXp;
        this.awardFrequency = awardFrequency;
        this.type = type;
        this.alreadyMapped = alreadyMapped;
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

    public boolean isAlreadyMapped() {
        return alreadyMapped;
    }

    public void setAlreadyMapped(boolean alreadyMapped) {
        this.alreadyMapped = alreadyMapped;
    }
}
