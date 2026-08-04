package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.admin.dto.request.CreateDepartmentRequest;
import com.pragatix.entity.Department;
import com.pragatix.entity.Section;
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
@Tag(name = "AdminDepartmentController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminDepartmentController {
    private static final Logger log = LoggerFactory.getLogger(AdminDepartmentController.class);

    private final AdminDepartmentService adminDepartmentService;

    public AdminDepartmentController(AdminDepartmentService adminDepartmentService) {
        this.adminDepartmentService = adminDepartmentService;
    }

    @GetMapping("/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List Departments")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllDepartments() {
        return adminDepartmentService.getAllDepartments();
    }

    @PostMapping("/departments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Department")
    public ResponseEntity<ApiResponse<Department>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        return adminDepartmentService.createDepartment(request);
    }

    @PutMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Department")
    public ResponseEntity<ApiResponse<Department>> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody CreateDepartmentRequest request) {
        return adminDepartmentService.updateDepartment(id, request);
    }

    @DeleteMapping("/departments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Department")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long id) {
        return adminDepartmentService.deleteDepartment(id);
    }

    @GetMapping("/departments/{id}/sections")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Sections of Department")
    public ResponseEntity<ApiResponse<List<Section>>> getSectionsOfDept(@PathVariable Long id) {
        return adminDepartmentService.getSectionsOfDept(id);
    }

    @PostMapping("/departments/{id}/sections")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Section for Department")
    public ResponseEntity<ApiResponse<Section>> createSection(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return adminDepartmentService.createSection(id, body);
    }

    @DeleteMapping("/departments/{id}/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete Section from Department")
    public ResponseEntity<ApiResponse<Void>> deleteSection(
            @PathVariable Long id,
            @PathVariable Long sectionId) {
        return adminDepartmentService.deleteSection(id, sectionId);
    }

    @GetMapping("/departments/class-coordinators")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all class coordinators mapped by department and section")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassCoordinators() {
        return adminDepartmentService.getClassCoordinators();
    }

}
