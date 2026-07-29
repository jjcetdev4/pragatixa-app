package com.pragatix.student;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Badge;
import com.pragatix.entity.Level;
import com.pragatix.modules.student.dto.response.StudentBadgeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Levels & Badges", description = "Endpoints for student levels and badge claims")
@SecurityRequirement(name = "bearerAuth")
public class LevelBadgeController {

    private final LevelBadgeService levelBadgeService;
    private final com.pragatix.modules.authentication.security.StudentAuthResolver studentAuthResolver;

    public LevelBadgeController(LevelBadgeService levelBadgeService,
            com.pragatix.modules.authentication.security.StudentAuthResolver studentAuthResolver) {
        this.levelBadgeService = levelBadgeService;
        this.studentAuthResolver = studentAuthResolver;
    }

    /** GET /api/v1/levels – Get all 8 levels */
    @GetMapping("/levels")
    @Operation(summary = "Get All Levels", description = "Returns the JJCET 8-level individual progression stepper limits.")
    public ResponseEntity<ApiResponse<List<Level>>> getAllLevels() {
        return ResponseEntity.ok(ApiResponse.ok(levelBadgeService.getAllLevels()));
    }

    /**
     * GET /api/v1/levels/student/{regNo}/current – Get student's current level by
     * roll number
     */
    @GetMapping("/levels/student/{regNo}/current")
    @Operation(summary = "Get Student's Current Level", description = "Determines a student's active level based on their current XP score.")
    public ResponseEntity<ApiResponse<Level>> getCurrentLevelForStudent(@PathVariable String regNo) {
        return levelBadgeService.getCurrentLevelForStudent(regNo)
                .map(level -> ResponseEntity.ok(ApiResponse.ok(level)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/levels/me/current – Get currently logged in student's active
     * level
     */
    @GetMapping("/levels/me/current")
    @Operation(summary = "Get Current Logged-in Student's Level")
    public ResponseEntity<ApiResponse<Level>> getCurrentLoggedInLevel() {
        String regNo = studentAuthResolver.getLoggedInStudent().getRegNo();
        return levelBadgeService.getCurrentLevelForStudent(regNo)
                .map(level -> ResponseEntity.ok(ApiResponse.ok(level)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/v1/badges – Get all configured badges */
    @GetMapping("/badges")
    @Operation(summary = "Get All Badges", description = "Returns all badges in all 5 tiers.")
    public ResponseEntity<ApiResponse<List<Badge>>> getAllBadges() {
        return ResponseEntity.ok(ApiResponse.ok(levelBadgeService.getAllBadges()));
    }

    /**
     * GET /api/v1/badges/student/me – Get logged-in student's earned & pending
     * badges
     */
    @GetMapping("/badges/student/me")
    @Operation(summary = "Get My Badges", description = "Returns all earned and pending badge claims for the active student session.")
    public ResponseEntity<ApiResponse<List<StudentBadgeResponse>>> getMyBadges() {
        String regNo = studentAuthResolver.getLoggedInStudent().getRegNo();
        return ResponseEntity.ok(ApiResponse.ok(levelBadgeService.getBadgesForStudent(regNo)));
    }

    /** GET /api/v1/badges/student/{regNo} – Get a specific student's badges */
    @GetMapping("/badges/student/{regNo}")
    @Operation(summary = "Get Badges by Student ID")
    public ResponseEntity<ApiResponse<List<StudentBadgeResponse>>> getBadgesForStudent(@PathVariable String regNo) {
        return ResponseEntity.ok(ApiResponse.ok(levelBadgeService.getBadgesForStudent(regNo)));
    }

    /** POST /api/v1/badges/submit – Student submits a claim for a badge */
    @PostMapping("/badges/submit")
    @Operation(summary = "Claim Badge", description = "Student submits a badge claim with evidence URL.")
    public ResponseEntity<ApiResponse<StudentBadgeResponse>> submitBadgeClaim(@RequestBody ClaimBadgeRequest request) {
        String regNo = studentAuthResolver.getLoggedInStudent().getRegNo();

        ApiResponse<StudentBadgeResponse> response = levelBadgeService.submitBadgeClaim(regNo, request.getBadgeName(),
                request.getEvidenceUrl());

        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /** PUT /api/v1/badges/{id}/approve – Faculty/Admin approves a badge claim */
    @PutMapping("/badges/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Approve Badge Claim", description = "Approves a pending badge claim. Requires Faculty or Admin role.")
    public ResponseEntity<ApiResponse<StudentBadgeResponse>> approveBadgeClaim(@PathVariable Long id) {
        String approvedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<StudentBadgeResponse> response = levelBadgeService.approveBadgeClaim(id, approvedBy);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /** PUT /api/v1/badges/{id}/reject – Faculty/Admin rejects a badge claim */
    @PutMapping("/badges/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Reject Badge Claim", description = "Rejects a pending badge claim. Requires Faculty or Admin role.")
    public ResponseEntity<ApiResponse<StudentBadgeResponse>> rejectBadgeClaim(@PathVariable Long id) {
        String rejectedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<StudentBadgeResponse> response = levelBadgeService.rejectBadgeClaim(id, rejectedBy);
        return response.isSuccess()
                ? ResponseEntity.ok(response)
                : ResponseEntity.badRequest().body(response);
    }

    /** GET /api/v1/badges/pending – Get all pending student badge claims */
    @GetMapping("/badges/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Get Pending Badge Claims", description = "Returns all student badge claims with PENDING status. Requires Faculty or Admin role.")
    public ResponseEntity<ApiResponse<List<StudentBadgeResponse>>> getPendingBadgeClaims() {
        return ResponseEntity.ok(ApiResponse.ok(levelBadgeService.getPendingBadgeClaims()));
    }

    // Request DTO inside the controller for self-containment
    public static class ClaimBadgeRequest {
        private String badgeName;
        private String evidenceUrl;

        public String getBadgeName() {
            return badgeName;
        }

        public void setBadgeName(String badgeName) {
            this.badgeName = badgeName;
        }

        public String getEvidenceUrl() {
            return evidenceUrl;
        }

        public void setEvidenceUrl(String evidenceUrl) {
            this.evidenceUrl = evidenceUrl;
        }
    }
}
