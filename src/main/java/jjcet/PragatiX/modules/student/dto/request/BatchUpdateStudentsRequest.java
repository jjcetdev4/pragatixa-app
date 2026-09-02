package jjcet.PragatiX.modules.student.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BatchUpdateStudentsRequest {

    @NotEmpty(message = "studentIds list cannot be empty")
    private List<Long> studentIds;

    private Long departmentId;
    private Long yearId;
    private String year;
    private Long semesterId;
    private String semester;
    private Long sectionId;

    public BatchUpdateStudentsRequest() {}

    public List<Long> getStudentIds() {
        return studentIds;
    }

    public void setStudentIds(List<Long> studentIds) {
        this.studentIds = studentIds;
    }

    public Long getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }

    public Long getYearId() {
        return yearId;
    }

    public void setYearId(Long yearId) {
        this.yearId = yearId;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public Long getSemesterId() {
        return semesterId;
    }

    public void setSemesterId(Long semesterId) {
        this.semesterId = semesterId;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public void setSectionId(Long sectionId) {
        this.sectionId = sectionId;
    }
}
