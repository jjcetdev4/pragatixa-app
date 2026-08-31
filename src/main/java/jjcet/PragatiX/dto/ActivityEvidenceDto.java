package jjcet.PragatiX.dto;

import jjcet.PragatiX.entity.ActivityEvidence;

import java.time.LocalDateTime;

public class ActivityEvidenceDto {
    private Long id;
    private String name;
    private String description;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ActivityEvidenceDto() {
    }

    public ActivityEvidenceDto(Long id, String name, String description, Integer displayOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ActivityEvidenceDto fromEntity(ActivityEvidence evidence) {
        if (evidence == null) return null;
        return new ActivityEvidenceDto(
                evidence.getId(),
                evidence.getName(),
                evidence.getDescription(),
                evidence.getDisplayOrder(),
                evidence.getCreatedAt(),
                evidence.getUpdatedAt()
        );
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
