package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.web.bind.annotation.RestController;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminDashboardController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminDashboardController {
    private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Dashboard Stats", description = "Get overview metrics for admin dashboard.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        return adminDashboardService.getDashboardStats();
    }

}
