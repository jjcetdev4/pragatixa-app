package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.AcademicYear;
import com.pragatix.entity.Year;
import com.pragatix.entity.Semester;
import com.pragatix.entity.Gender;
import com.pragatix.entity.Section;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminLookupController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminLookupController {
    private static final Logger log = LoggerFactory.getLogger(AdminLookupController.class);

    private final AdminLookupService adminLookupService;

    public AdminLookupController(AdminLookupService adminLookupService) {
        this.adminLookupService = adminLookupService;
    }

    @GetMapping("/academic-years")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Academic Years")
    public ResponseEntity<ApiResponse<List<AcademicYear>>> getAllAcademicYears() {
        return adminLookupService.getAllAcademicYears();
    }

    @GetMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Years")
    public ResponseEntity<ApiResponse<List<Year>>> getAllYears() {
        return adminLookupService.getAllYears();
    }

    @GetMapping("/semesters")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Semesters")
    public ResponseEntity<ApiResponse<List<Semester>>> getAllSemesters() {
        return adminLookupService.getAllSemesters();
    }

    @GetMapping("/genders")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Genders")
    public ResponseEntity<ApiResponse<List<Gender>>> getAllGenders() {
        return adminLookupService.getAllGenders();
    }

    @GetMapping("/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Sections")
    public ResponseEntity<ApiResponse<List<Section>>> getAllSections(@RequestParam(required = false) Long departmentId) {
        return adminLookupService.getAllSections(departmentId);
    }

}
