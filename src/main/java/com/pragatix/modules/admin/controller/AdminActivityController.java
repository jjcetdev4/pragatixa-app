package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.modules.activity.dto.response.MyActivityResponse;
import com.pragatix.modules.activity.dto.response.GroupedActivityResponse;
import com.pragatix.modules.activity.dto.request.AssignmentRequest;
import com.pragatix.modules.activity.dto.response.ActivityAssignmentResponse;
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
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminActivityController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminActivityController {
    private static final Logger log = LoggerFactory.getLogger(AdminActivityController.class);

    private final AdminActivityService adminActivityService;
    private final ActivityAssignmentService activityAssignmentService;

    public AdminActivityController(AdminActivityService adminActivityService,
            ActivityAssignmentService activityAssignmentService) {
        this.adminActivityService = adminActivityService;
        this.activityAssignmentService = activityAssignmentService;
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
    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesBySubgroup(
            @PathVariable Long subgroupId,
            @RequestParam(required = false) com.pragatix.enums.AcademicYear academicYear) {
        return adminActivityService.getActivitiesBySubgroup(subgroupId, academicYear);
    }

    @GetMapping("/stages/{stageId}/activities")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Get all activities of a stage")
    public ResponseEntity<ApiResponse<List<Activity>>> getActivitiesByStage(
            @PathVariable Long stageId,
            @RequestParam(required = false) String subgroup,
            @RequestParam(required = false) com.pragatix.enums.AcademicYear academicYear) {
        return adminActivityService.getActivitiesByStage(stageId, subgroup, academicYear);
    }

    @GetMapping("/activities")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all activities globally")
    public ResponseEntity<ApiResponse<List<Activity>>> getAllActivities(
            @RequestParam(required = false) String subgroup,
            @RequestParam(required = false) com.pragatix.enums.AcademicYear academicYear) {
        return adminActivityService.getAllActivities(subgroup, academicYear);
    }

    @GetMapping("/activities/grouped")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all activities grouped by subgroup")
    public ResponseEntity<ApiResponse<List<GroupedActivityResponse>>> getGroupedActivities(
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String subgroup,
            @RequestParam(required = false) com.pragatix.enums.AcademicYear academicYear) {
        return adminActivityService.getGroupedActivities(stageId, subgroup, academicYear);
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
    @Operation(summary = "Assign departments/sections/faculty to an activity (Bulk)")
    public ResponseEntity<ApiResponse<Void>> assignActivity(
            @PathVariable Long id,
            @RequestParam(required = false) Long stageId,
            @RequestBody Map<String, Object> body) {
        if (stageId != null && body != null && !body.containsKey("stageId")) {
            body.put("stageId", stageId);
        }
        return activityAssignmentService.assignActivity(id, body);
    }

    @GetMapping("/activities/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLASS_COORDINATOR', 'TEACHER')")
    @Operation(summary = "Get all assignments for an activity")
    public ResponseEntity<ApiResponse<List<ActivityAssignmentResponse>>> getAssignments(
            @PathVariable Long id,
            @RequestParam(required = false) Long stageId) {
        return activityAssignmentService.getAssignments(id, stageId);
    }

    @PostMapping("/activities/{id}/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLASS_COORDINATOR')")
    @Operation(summary = "Add a single assignment to an activity")
    public ResponseEntity<ApiResponse<ActivityAssignmentResponse>> addAssignment(
            @PathVariable Long id,
            @RequestParam(required = false) Long stageId,
            @RequestBody AssignmentRequest request) {
        
        log.info("========================");
        log.info("Incoming Assignment Request");
        log.info("activityId: {}", id);
        log.info("stageId: {}", stageId);
        log.info("teacherId: {}", request.getTeacherId());
        log.info("departmentId: {}", request.getDepartmentId());
        log.info("sectionId: {}", request.getSectionId());
        log.info("assignmentType: {}", request.getScope());
        log.info("assignmentMode: UNKNOWN");
        log.info("JWT User: {}", org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() != null ? org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName() : "Anonymous");
        log.info("========================");
        
        return activityAssignmentService.addAssignment(id, stageId, request);
    }

    @DeleteMapping("/activities/assignments/{assignmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CLASS_COORDINATOR')")
    @Operation(summary = "Remove a single assignment")
    public ResponseEntity<ApiResponse<Void>> removeAssignment(@PathVariable Long assignmentId) {
        return activityAssignmentService.removeAssignment(assignmentId);
    }

    @DeleteMapping("/activities/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an activity")
    public ResponseEntity<ApiResponse<Void>> deleteActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        return adminActivityService.deleteActivity(activityId, force);
    }

    @GetMapping("/frequencies/custom")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all custom award frequencies")
    public ResponseEntity<ApiResponse<List<com.pragatix.entity.CustomFrequency>>> getCustomFrequencies() {
        return adminActivityService.getCustomFrequencies();
    }

    @PostMapping("/frequencies/custom")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a custom award frequency")
    public ResponseEntity<ApiResponse<com.pragatix.entity.CustomFrequency>> createCustomFrequency(
            @RequestBody Map<String, Object> payload) {
        return adminActivityService.createCustomFrequency(payload);
    }

    @PostMapping("/stages/{stageId}/activities/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Map an existing activity to a stage")
    public ResponseEntity<ApiResponse<Void>> mapActivityToStage(
            @PathVariable Long stageId,
            @PathVariable Long activityId,
            @RequestParam String subgroup) {
        return adminActivityService.mapActivityToStage(stageId, activityId, subgroup);
    }

    @DeleteMapping("/stages/{stageId}/activities/{activityId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove an activity from a stage without deleting it")
    public ResponseEntity<ApiResponse<Void>> unmapActivityFromStage(
            @PathVariable Long stageId,
            @PathVariable Long activityId) {
        return adminActivityService.unmapActivityFromStage(stageId, activityId);
    }

    @DeleteMapping("/activities/{activityId}/assignments/clear")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remove all faculty assignments for an activity")
    public ResponseEntity<ApiResponse<Void>> clearAssignments(
            @PathVariable Long activityId,
            @RequestParam(required = false) Long stageId) {
        return activityAssignmentService.clearAssignments(activityId, stageId);
    }
}
