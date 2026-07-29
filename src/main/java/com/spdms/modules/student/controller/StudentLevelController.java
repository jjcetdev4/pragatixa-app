package com.pragatix.modules.student.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.student.dto.response.StudentProgressionDto;
import com.pragatix.modules.student.service.StudentLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/student-level")
@Tag(name = "Student Level Progression", description = "Endpoints for student level and pathway progression")
public class StudentLevelController {

    private final StudentLevelService studentLevelService;

    public StudentLevelController(StudentLevelService studentLevelService) {
        this.studentLevelService = studentLevelService;
    }

    @GetMapping("/progression")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get the student's current level progression, total XP, and timeline")
    public ResponseEntity<ApiResponse<StudentProgressionDto>> getStudentProgression() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            StudentProgressionDto progression = studentLevelService.getStudentProgression(username);
            return ResponseEntity.ok(ApiResponse.ok("Progression fetched successfully", progression));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch progression", e.getMessage()));
        }
    }
}
