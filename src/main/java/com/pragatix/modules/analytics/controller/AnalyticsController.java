package com.pragatix.modules.analytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERADMIN')")
public class AnalyticsController {

    @GetMapping("/student")
    public ResponseEntity<Map<String, String>> getStudentAnalytics() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ready");
        response.put("message", "Analytics module placeholder");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/teacher")
    public ResponseEntity<Map<String, String>> getTeacherAnalytics() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ready");
        response.put("message", "Analytics module placeholder");
        return ResponseEntity.ok(response);
    }
}
