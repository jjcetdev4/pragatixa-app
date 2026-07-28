package com.spdms.modules.admin.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.modules.activity.dto.request.ActivityStageRequest;
import com.spdms.modules.activity.dto.response.ActivityStageResponse;
import com.spdms.modules.activity.service.ActivityStageService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import com.spdms.modules.admin.service.*;
import com.spdms.modules.admin.mapper.*;

@Service
public class AdminStageService {
    private static final Logger log = LoggerFactory.getLogger(AdminStageService.class);

    private final ActivityStageService activityStageService;
    private final com.spdms.modules.student.repository.StudentRepository studentRepository;
    private final com.spdms.modules.student.service.XpEngineService xpEngineService;
    private final com.spdms.modules.authentication.repository.UserRepository userRepository;

    public AdminStageService(ActivityStageService activityStageService,
                             com.spdms.modules.student.repository.StudentRepository studentRepository,
                             com.spdms.modules.student.service.XpEngineService xpEngineService,
                             com.spdms.modules.authentication.repository.UserRepository userRepository) {
        this.activityStageService = activityStageService;
        this.studentRepository = studentRepository;
        this.xpEngineService = xpEngineService;
        this.userRepository = userRepository;
    }

    public ResponseEntity<ApiResponse<Void>> evaluatePromotions() {
        List<com.spdms.entity.Student> activeStudents = studentRepository.findByActiveTrue();
        int evaluated = 0;
        for (com.spdms.entity.Student student : activeStudents) {
            xpEngineService.evaluateStagePromotion(student);
            evaluated++;
        }
        return ResponseEntity.ok(ApiResponse.ok("Evaluated stage promotions for " + evaluated + " active students.", null));
    }

    public ResponseEntity<ApiResponse<List<ActivityStageResponse>>> getAllStages(String academicYear) {
        com.spdms.entity.AssignedAcademicYear resolvedYear = resolveYear(academicYear);
        List<ActivityStageResponse> stages = activityStageService.getAllStages(resolvedYear);
        return ResponseEntity.ok(ApiResponse.ok(stages));
    }

    public ResponseEntity<ApiResponse<ActivityStageResponse>> createStage(
            @Valid @RequestBody ActivityStageRequest request, String academicYear) {
        if (academicYear == null || academicYear.trim().isEmpty()) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
                throw new IllegalArgumentException("Academic Year is required for Super Admin.");
            }
        }
        com.spdms.entity.AssignedAcademicYear resolvedYear = resolveYear(academicYear);
        ActivityStageResponse saved = activityStageService.createStage(request, resolvedYear);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Stage created successfully", saved));
    }

    public ResponseEntity<ApiResponse<ActivityStageResponse>> getStage(@PathVariable Long id) {
        // You might want to also verify if the returned stage matches the resolvedYear if needed.
        return activityStageService.getStageById(id)
                .map(stage -> ResponseEntity.ok(ApiResponse.ok(stage)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Stage not found")));
    }

    public ResponseEntity<ApiResponse<ActivityStageResponse>> editStage(
            @PathVariable Long id,
            @Valid @RequestBody ActivityStageRequest request, String academicYear) {
        if (academicYear == null || academicYear.trim().isEmpty()) {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"))) {
                throw new IllegalArgumentException("Academic Year is required for Super Admin.");
            }
        }
        com.spdms.entity.AssignedAcademicYear resolvedYear = resolveYear(academicYear);
        ActivityStageResponse updated = activityStageService.updateStage(id, request, resolvedYear);
        return ResponseEntity.ok(ApiResponse.ok("Stage updated successfully", updated));
    }

    private com.spdms.entity.AssignedAcademicYear resolveYear(String requestedYearStr) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User is not authenticated");
        }
        String username = auth.getName();
        com.spdms.entity.User user = userRepository
            .findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));

        boolean isSuperAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"));
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"));

        if (isSuperAdmin) {
            if (requestedYearStr == null || requestedYearStr.trim().isEmpty()) {
                throw new IllegalArgumentException("Academic Year is required for Super Admin.");
            }
            return com.spdms.entity.AssignedAcademicYear.valueOf(requestedYearStr);
        } else if (isAdmin) {
            if (user.getAssignedAcademicYear() == null) {
                throw new IllegalStateException("Year Admin does not have an assigned academic year.");
            }
            return user.getAssignedAcademicYear();
        } else {
            // For students or teachers, fallback to their assigned year if applicable.
            // Simplified here: if requestedYearStr is provided, use it, else default to FIRST_YEAR or throw.
            if (requestedYearStr != null && !requestedYearStr.isEmpty()) {
                return com.spdms.entity.AssignedAcademicYear.valueOf(requestedYearStr);
            }
            throw new SecurityException("User role requires an academic year to fetch stages.");
        }
    }

    public ResponseEntity<ApiResponse<Map<String, Object>>> getStageReport(@PathVariable Long id) {
        try {
            Map<String, Object> report = activityStageService.getStageReport(id);
            return ResponseEntity.ok(ApiResponse.ok(report));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    public ResponseEntity<ApiResponse<Void>> deleteStage(@PathVariable Long id) {
        activityStageService.deleteStage(id);
        return ResponseEntity.ok(ApiResponse.ok("Stage deleted successfully", null));
    }

}
