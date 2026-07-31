package com.pragatix.modules.student.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.AwardXpRequest;
import com.pragatix.modules.student.dto.response.MyActivityStudentsResponse;
import com.pragatix.modules.student.service.StudentActivityQueryService;
import com.pragatix.modules.student.service.StudentXpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Student XP", description = "Endpoints for managing student XP awards")
public class StudentXpController {

    private final StudentXpService studentXpService;
    private final StudentActivityQueryService queryService;

    public StudentXpController(StudentXpService studentXpService, StudentActivityQueryService queryService) {
        this.studentXpService = studentXpService;
        this.queryService = queryService;
    }

    @GetMapping("/my-activities/{activityId}/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get distinct years assigned to the activity that the teacher has permission to view")
    public ResponseEntity<ApiResponse<List<String>>> getYearsForActivity(@PathVariable Long activityId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return queryService.getYearsForActivity(activityId, username);
    }

    @GetMapping("/my-activities/{activityId}/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get distinct departments assigned to the activity/year that the teacher has permission to view")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDepartmentsForActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) String year) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return queryService.getDepartmentsForActivity(activityId, year, username);
    }

    @GetMapping("/my-activities/{activityId}/sections")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get distinct sections assigned to the activity/year/department that the teacher has permission to view")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getSectionsForActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) String year,
            @RequestParam Long departmentId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return queryService.getSectionsForActivity(activityId, year, departmentId, username);
    }

    @GetMapping("/my-activities/{activityId}/students")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Get list of students eligible for the given assigned activity")
    public ResponseEntity<ApiResponse<MyActivityStudentsResponse>> getStudentsForActivity(
            @PathVariable Long activityId,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long sectionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return queryService.getStudentsForActivity(activityId, year, departmentId, sectionId, username);
    }

    @PostMapping("/student-xp/award")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Award XP points to a student for a specific activity")
    public ResponseEntity<ApiResponse<Void>> awardStudentXp(@RequestBody AwardXpRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return studentXpService.awardStudentXp(request, username);
    }

    @PostMapping("/student-xp/award/batch")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Award XP points to multiple students for a specific activity")
    public ResponseEntity<?> awardStudentXpBatch(@RequestBody AwardXpRequest request) {
        System.out.println("CONTROLLER DEBUG: Received studentIds: " + request.getStudentIds());
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return studentXpService.awardStudentXpBatch(request, username);
    }
}
