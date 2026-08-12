package com.pragatix.modules.superadmin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.superadmin.dto.YearAdminResponse;
import com.pragatix.modules.superadmin.dto.AssignAcademicYearRequest;
import com.pragatix.modules.superadmin.service.SuperAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/superadmin")
@Tag(name = "SuperAdminController", description = "Super Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @GetMapping("/year-admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Get all year admins")
    public ResponseEntity<ApiResponse<List<YearAdminResponse>>> getYearAdmins() {
        return superAdminService.getYearAdmins();
    }

    @PostMapping("/year-admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create new Year Admin")
    public ResponseEntity<ApiResponse<YearAdminResponse>> createYearAdmin(
            @org.springframework.web.bind.annotation.RequestBody com.pragatix.modules.superadmin.dto.CreateYearAdminRequest request) {
        return superAdminService.createYearAdmin(request);
    }

    @PutMapping("/year-admins/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update Year Admin details")
    public ResponseEntity<ApiResponse<YearAdminResponse>> updateYearAdmin(
            @PathVariable Long id,
            @RequestBody com.pragatix.modules.superadmin.dto.UpdateYearAdminRequest request) {
        return superAdminService.updateYearAdmin(id, request);
    }

    @DeleteMapping("/year-admins/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete Year Admin")
    public ResponseEntity<ApiResponse<Void>> deleteYearAdmin(@PathVariable Long id) {
        return superAdminService.deleteYearAdmin(id);
    }

    @PostMapping("/cache/refresh")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Refresh DB Cache")
    public ResponseEntity<ApiResponse<Void>> refreshDbCache() {
        return superAdminService.refreshDbCache();
    }
}
