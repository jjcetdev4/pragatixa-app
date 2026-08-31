package jjcet.PragatiX.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ActivityEvidenceCreateUpdateDto {

    @NotBlank(message = "Evidence name is required")
    @Size(max = 100, message = "Evidence name must not exceed 100 characters")
    private String name;

    private String description;

    private Integer displayOrder = 0;

    public ActivityEvidenceCreateUpdateDto() {
    }

    public ActivityEvidenceCreateUpdateDto(String name, String description, Integer displayOrder) {
        this.name = name;
        this.description = description;
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

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
