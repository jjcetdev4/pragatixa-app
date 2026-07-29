package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.activity.dto.request.ActivityStageRequest;
import com.pragatix.modules.activity.dto.response.ActivityStageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminStageController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminStageController {
    private static final Logger log = LoggerFactory.getLogger(AdminStageController.class);

    private final AdminStageService adminStageService;

    public AdminStageController(AdminStageService adminStageService) {
        this.adminStageService = adminStageService;
    }

    @GetMapping("/stages")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get all activity stages with subgroups")
    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getAllStages(
            @RequestParam(required = false) com.pragatix.enums.AcademicYear academicYear) {
        return adminStageService.getAllStages(academicYear);
    }

    @PostMapping("/stages")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new stage")
    public ResponseEntity<ApiResponse<ActivityStageResponse>> createStage(
            @Valid @RequestBody ActivityStageRequest request) {
        System.out.println("Controller: Incoming Academic Year - " + request.getAcademicYear());
        System.out.println("Controller: Incoming Stage Name - " + request.getName());
        return adminStageService.createStage(request);
    }

    @GetMapping("/stages/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get activity stage by ID")
    public ResponseEntity<ApiResponse<ActivityStageResponse>> getStage(@PathVariable Long id) {
        return adminStageService.getStage(id);
    }

    @PutMapping("/stages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an existing stage")
    public ResponseEntity<ApiResponse<ActivityStageResponse>> editStage(
            @PathVariable Long id,
            @Valid @RequestBody ActivityStageRequest request) {
        System.out.println("Controller received Academic Year: " + request.getAcademicYear());
        return adminStageService.editStage(id, request);
    }

    @GetMapping("/stages/{id}/report")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get stage completion report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStageReport(@PathVariable Long id) {
        return adminStageService.getStageReport(id);
    }

    @DeleteMapping("/stages/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a stage and its subgroups")
    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable Long id) {
        return adminStageService.deleteStage(id);
    }

    @PostMapping("/stages/evaluate-promotions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Force evaluate stage promotions for all students")
    public ResponseEntity<ApiResponse<Void>> evaluatePromotions() {
        return adminStageService.evaluatePromotions();
    }

}
