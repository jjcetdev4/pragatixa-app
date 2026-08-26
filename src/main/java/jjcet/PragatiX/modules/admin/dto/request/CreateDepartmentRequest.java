package jjcet.PragatiX.modules.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jjcet.PragatiX.enums.DepartmentType;
import java.util.List;

public class CreateDepartmentRequest {
    @NotBlank(message = "Department name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Department code is required")
    @Size(max = 10)
    private String code;

    private String description;

    private DepartmentType departmentType;

    private Boolean supportsSections;

    private List<String> sections;

    public CreateDepartmentRequest() {
    }

    public DepartmentType getDepartmentType() {
        return departmentType;
    }

    public void setDepartmentType(DepartmentType departmentType) {
        this.departmentType = departmentType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSections() {
        return sections;
    }

    public void setSections(List<String> sections) {
        this.sections = sections;
    }

    public Boolean getSupportsSections() {
        return supportsSections;
    }

    public void setSupportsSections(Boolean supportsSections) {
        this.supportsSections = supportsSections;
    }
}
