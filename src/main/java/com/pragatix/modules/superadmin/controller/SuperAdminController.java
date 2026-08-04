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

    @PutMapping("/year-admins/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Assign academic year to admin")
    public ResponseEntity<ApiResponse<YearAdminResponse>> assignAcademicYear(
            @PathVariable Long id,
            @RequestBody AssignAcademicYearRequest request) {
        return superAdminService.assignAcademicYear(id, request);
    }
}
