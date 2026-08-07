package com.pragatix.modules.hod.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.hod.dto.HodDashboardResponse;
import com.pragatix.modules.hod.service.HodAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/hod/analytics")
public class HodAnalyticsController {

    private final HodAnalyticsService hodAnalyticsService;

    public HodAnalyticsController(HodAnalyticsService hodAnalyticsService) {
        this.hodAnalyticsService = hodAnalyticsService;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<HodDashboardResponse>> getDashboardData(
            @RequestParam(required = false) String year) {
        try {
            HodDashboardResponse data = hodAnalyticsService.getDashboardData(year);
            return ResponseEntity.ok(ApiResponse.ok("HOD Dashboard data fetched successfully", data));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
