package com.pragatix.modules.cc.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.modules.activity.dto.response.ActivityStageResponse;
import com.pragatix.modules.cc.dto.CCActivityAssignRequest;
import com.pragatix.modules.cc.dto.CCTeacherAssignRequest;
import com.pragatix.modules.cc.service.CCActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cc/activities")
@Tag(name = "CCActivityController", description = "Class Coordinator Activity Endpoints")
@SecurityRequirement(name = "bearerAuth")
public class CCActivityController {

    private final CCActivityService ccActivityService;

    public CCActivityController(CCActivityService ccActivityService) {
        this.ccActivityService = ccActivityService;
    }

    @GetMapping("/stages")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get all active stages available for Class Coordinator")
    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getStages(
            @RequestParam(required = false) String academicYear) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccActivityService.getStages(username, academicYear);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get all active activities available for Class Coordinator")
    public ResponseEntity<ApiResponse<List<Activity>>> getActiveActivities(
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String subgroup) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccActivityService.getActiveActivities(username, stageId, subgroup);
    }

    @GetMapping("/class-details")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get class details (Department, Year, Section) for the logged-in Class Coordinator")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCCClassDetails() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccActivityService.getCCClassDetails(username);
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get teachers belonging to the logged-in CC's department, year, and section")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassTeachers() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccActivityService.getClassTeachers(username);
    }

    @GetMapping({"/students", "/{activityId}/students"})
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get students belonging to the logged-in CC's department, year, and section")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getClassStudents(
            @PathVariable(required = false) Long activityId,
            @RequestParam(required = false) Long stageId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccActivityService.getClassStudents(username, activityId, stageId);
    }

    @PostMapping("/{activityId}/assign-teacher")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Assign an activity to one teacher in the CC's class")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignTeacher(
            @PathVariable Long activityId,
            @RequestBody CCTeacherAssignRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccActivityService.assignTeacherToActivity(username, activityId, request);
    }

    @PostMapping("/{activityId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Assign an activity in the CC's class")
    public ResponseEntity<ApiResponse<Map<String, Object>>> assignActivity(
            @PathVariable Long activityId,
            @RequestBody(required = false) CCActivityAssignRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return ccActivityService.assignActivity(username, activityId, request);
    }
}
