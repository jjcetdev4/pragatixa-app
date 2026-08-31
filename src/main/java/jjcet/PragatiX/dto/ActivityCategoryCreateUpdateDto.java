package jjcet.PragatiX.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ActivityCategoryCreateUpdateDto {

    @NotBlank(message = "Category name is required")
    @Size(min = 2, max = 100, message = "Category name must be between 2 and 100 characters")
    private String name;

    private String description;

    private String icon;

    private Integer displayOrder;

    public ActivityCategoryCreateUpdateDto() {
    }

    public ActivityCategoryCreateUpdateDto(String name, String description, String icon, Integer displayOrder) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.displayOrder = displayOrder;
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
}
