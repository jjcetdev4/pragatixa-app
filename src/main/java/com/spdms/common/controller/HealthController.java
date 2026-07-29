package com.spdms.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health check controller for CloudFront and AWS ALB target group health checks.
 */
@RestController
@Tag(name = "Health Check", description = "Health check endpoints for CloudFront / ALB monitoring")
public class HealthController {

    @GetMapping({"/api/health", "/api/actuator/health"})
    @Operation(summary = "Health status check", description = "Returns system health status for CloudFront/ALB target group checks")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "spdms-backend",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
