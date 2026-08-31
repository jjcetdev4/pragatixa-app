package jjcet.PragatiX.dto;

import jjcet.PragatiX.entity.ActivityCategory;

import java.time.LocalDateTime;

public class ActivityCategoryDto {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ActivityCategoryDto() {
    }

    public ActivityCategoryDto(Long id, String name, String description, String icon, Integer displayOrder, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ActivityCategoryDto fromEntity(ActivityCategory category) {
        if (category == null) return null;
        return new ActivityCategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIcon(),
                category.getDisplayOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt()
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
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
