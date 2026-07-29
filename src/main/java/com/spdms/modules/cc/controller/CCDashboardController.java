package com.pragatix.modules.cc.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.cc.service.CCDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cc/dashboard")
public class CCDashboardController {

    private final CCDashboardService ccDashboardService;

    public CCDashboardController(CCDashboardService ccDashboardService) {
        this.ccDashboardService = ccDashboardService;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccDashboardService.getDashboardStats(username);
    }
}
