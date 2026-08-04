package com.pragatix.modules.superadmin.dto;

import com.pragatix.enums.AcademicYear;
import jakarta.validation.constraints.NotNull;

public class AssignAcademicYearRequest {

    @NotNull(message = "Academic Year is required")
    private AcademicYear academicYear;

    public AssignAcademicYearRequest() {
    }

    public AssignAcademicYearRequest(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }
}
