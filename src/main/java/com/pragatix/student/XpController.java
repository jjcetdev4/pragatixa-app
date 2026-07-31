package com.pragatix.student;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.XpTransactionDto;
import com.pragatix.entity.XpTransaction;
import com.pragatix.dto.StreakResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/xp")
public class XpController {

    private final XpService xpService;
    private final com.pragatix.modules.authentication.security.StudentAuthResolver studentAuthResolver;

    public XpController(XpService xpService,
            com.pragatix.modules.authentication.security.StudentAuthResolver studentAuthResolver) {
        this.xpService = xpService;
        this.studentAuthResolver = studentAuthResolver;
    }

    /** GET /api/v1/xp/{regNo}/summary – Total + by category summary */
    @GetMapping("/{regNo}/summary")
    @Operation(summary = "Get XP Summary", description = "Returns total XP points earned by category.")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getXpSummary(@PathVariable String regNo) {
        System.out.println("Received Student ID: " + regNo);
        return ResponseEntity.ok(ApiResponse.ok(xpService.getXpSummary(regNo)));
    }

    /** GET /api/v1/xp/{regNo}/history – Paginated XP history */
    @GetMapping("/{regNo}/history")
    @Operation(summary = "Get Paginated XP History", description = "Returns a paginated list of XP transactions for a student.")
    public ResponseEntity<ApiResponse<Page<XpTransactionDto>>> getXpHistory(
            @PathVariable String regNo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(xpService.getXpHistory(regNo, page, size)));
    }

    /** GET /api/v1/xp/{regNo}/streaks – Get active student streaks */
    @GetMapping("/{regNo}/streaks")
    @Operation(summary = "Get Student Streaks", description = "Returns all coding, diary, and library streaks for a student.")
    public ResponseEntity<ApiResponse<List<StreakResponse>>> getStudentStreaks(@PathVariable String regNo) {
        return ResponseEntity.ok(ApiResponse.ok(xpService.getStudentStreaks(regNo)));
    }

    /** POST /api/v1/xp/submit – Student submits activity claim */
    @PostMapping("/submit")
    @Operation(summary = "Submit XP Claim", description = "Allows a student to submit evidence link for an activity.")
    public ResponseEntity<ApiResponse<XpTransaction>> submitXpClaim(@RequestBody ClaimSubmissionRequest request) {
        String regNo = studentAuthResolver.getLoggedInStudent().getRegNo();
        ApiResponse<XpTransaction> response = xpService.submitXpClaim(
                regNo,
                request.getCategory(),
                request.getActivityName(),
                request.getXpPoints(),
                request.getEvidenceUrl());
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /** PUT /api/v1/xp/{id}/approve – Faculty/Admin approves XP claim */
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Approve XP Claim", description = "Approves a pending student XP claim. Requires Faculty or Admin role.")
    public ResponseEntity<ApiResponse<XpTransaction>> approveXpClaim(@PathVariable Long id) {
        String approvedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<XpTransaction> response = xpService.approveXpClaim(id, approvedBy);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /** PUT /api/v1/xp/{id}/reject – Faculty/Admin rejects XP claim */
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Reject XP Claim", description = "Rejects a pending student XP claim. Requires Faculty or Admin role.")
    public ResponseEntity<ApiResponse<XpTransaction>> rejectXpClaim(@PathVariable Long id) {
        String approvedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<XpTransaction> response = xpService.rejectXpClaim(id, approvedBy);
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    /** POST /api/v1/xp/penalty – Faculty logs a violation penalty */
    @PostMapping("/penalty")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "Log Violation Penalty", description = "Deducts XP points from a student for a discipline infraction. Requires Faculty or Admin role.")
    public ResponseEntity<ApiResponse<XpTransaction>> logViolation(@RequestBody LogViolationRequest request) {
        String appliedBy = SecurityContextHolder.getContext().getAuthentication().getName();
        ApiResponse<XpTransaction> response = xpService.logViolation(
                request.getRegNo(),
                request.getViolationType(),
                request.getXpPenalty(),
                appliedBy,
                request.getDescription());
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Request DTOs
    public static class ClaimSubmissionRequest {
        private String category;
        private String activityName;
        private int xpPoints;
        private String evidenceUrl;

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getActivityName() {
            return activityName;
        }

        public void setActivityName(String activityName) {
            this.activityName = activityName;
        }

        public int getXpPoints() {
            return xpPoints;
        }

        public void setXpPoints(int xpPoints) {
            this.xpPoints = xpPoints;
        }

        public String getEvidenceUrl() {
            return evidenceUrl;
        }

        public void setEvidenceUrl(String evidenceUrl) {
            this.evidenceUrl = evidenceUrl;
        }
    }

    public static class LogViolationRequest {
        private String regNo;
        private String violationType;
        private int xpPenalty;
        private String description;

        public String getRegNo() {
            return regNo;
        }

        public void setRegNo(String regNo) {
            this.regNo = regNo;
        }

        public String getViolationType() {
            return violationType;
        }

        public void setViolationType(String violationType) {
            this.violationType = violationType;
        }

        public int getXpPenalty() {
            return xpPenalty;
        }

        public void setXpPenalty(int xpPenalty) {
            this.xpPenalty = xpPenalty;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
