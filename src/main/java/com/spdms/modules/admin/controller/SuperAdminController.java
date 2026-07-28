package com.spdms.modules.admin.controller;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.User;
import com.spdms.modules.admin.dto.request.YearAdminRequest;
import com.spdms.modules.admin.dto.response.YearAdminResponse;
import com.spdms.modules.admin.service.SuperAdminService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/superadmin/year-admins")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
    }

    @GetMapping
    public ApiResponse<List<YearAdminResponse>> getAllYearAdmins() {
        List<YearAdminResponse> response = superAdminService.getAllYearAdmins().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ApiResponse.ok("Year Admins retrieved successfully", response);
    }

    @PostMapping
    public ApiResponse<YearAdminResponse> createYearAdmin(@RequestBody YearAdminRequest request) {
        User user = superAdminService.createYearAdmin(
                request.getFullName(),
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getPhone(),
                request.getAssignedAcademicYear(),
                request.isActive()
        );
        return ApiResponse.ok("Year Admin created successfully", mapToResponse(user));
    }

    @PutMapping("/{id}")
    public ApiResponse<YearAdminResponse> updateYearAdmin(@PathVariable Long id, @RequestBody YearAdminRequest request) {
        User user = superAdminService.updateYearAdmin(
                id,
                request.getFullName(),
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getPhone(),
                request.getAssignedAcademicYear(),
                request.isActive()
        );
        return ApiResponse.ok("Year Admin updated successfully", mapToResponse(user));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteYearAdmin(@PathVariable Long id, java.security.Principal principal) {
        superAdminService.deleteYearAdmin(id, principal.getName());
        return ApiResponse.ok("Year Admin deleted successfully", null);
    }

    private YearAdminResponse mapToResponse(User user) {
        return new YearAdminResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getAssignedAcademicYear(),
                user.isActive()
        );
    }
}
