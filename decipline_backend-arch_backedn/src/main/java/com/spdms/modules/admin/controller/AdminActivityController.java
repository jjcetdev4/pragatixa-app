package com.spdms.modules.admin.controller;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Activity;
import com.spdms.modules.activity.dto.response.MyActivityResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;
import com.spdms.modules.admin.service.*;
import com.spdms.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminActivityController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminActivityController {
    private static final Logger log = LoggerFactory.getLogger(AdminActivityController.class);

    private final AdminActivityService adminActivityService;

    public AdminActivityController(AdminActivityService adminActivityService) {
        this.adminActivityService = adminActivityService;
    }

    @GetMapping("/my-activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get activities assigned to the currently logged in teacher")
    public ResponseEntity<ApiResponse<List<MyActivityResponse>>> getMyActivities() {
        return adminActivityService.getMyActivities();
    }

    @GetMapping("/subgroups/{subgroupId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get all activities of a subgroup")
    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesBySubgroup(@PathVariable Long subgroupId) {
        return adminActivityService.getActivitiesBySubgroup(subgroupId);
    }

    @PostMapping("/subgroups/{subgroupId}/activities")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new activity under a subgroup")
    public ResponseEntity<ApiResponse<Activity>> createActivity(
                @PathVariable Long subgroupId,
                @RequestBody Map<String, Object> body) {
        return adminActivityService.createActivity(subgroupId, body);
    }

    @PutMapping("/activities/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update an activity")
    public ResponseEntity<ApiResponse<Activity>> updateActivity(
                @PathVariable Long activityId,
                @RequestBody Map<String, Object> body) {
        return adminActivityService.updateActivity(activityId, body);
    }

    @PostMapping(value = { "/activities/{id}/assign", "/activity/{id}/assign" })
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign departments/sections/faculty to an activity")
    public ResponseEntity<ApiResponse<Void>> assignActivity(
                @PathVariable Long id,
                @RequestBody Map<String, Object> body) {
        return adminActivityService.assignActivity(id, body);
    }

    @DeleteMapping("/activities/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an activity")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(@PathVariable Long activityId) {
        return adminActivityService.deleteActivity(activityId);
    }

    @GetMapping("/frequencies/custom")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all custom award frequencies")
    public ResponseEntity<ApiResponse<List<com.spdms.entity.CustomFrequency>>> getCustomFrequencies() {
        return adminActivityService.getCustomFrequencies();
    }

    @PostMapping("/frequencies/custom")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a custom award frequency")
    public ResponseEntity<ApiResponse<com.spdms.entity.CustomFrequency>> createCustomFrequency(
                @RequestBody Map<String, Object> payload) {
        return adminActivityService.createCustomFrequency(payload);
    }

}
