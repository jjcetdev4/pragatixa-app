package com.spdms.student;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.XpTransaction;
import com.spdms.entity.Streak;
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
    private final com.spdms.security.StudentAuthResolver studentAuthResolver;

    public XpController(XpService xpService, com.spdms.security.StudentAuthResolver studentAuthResolver) {
        this.xpService = xpService;
        this.studentAuthResolver = studentAuthResolver;
    }

    /** GET /api/v1/xp/{studentId}/summary – Total + by category summary */
    @GetMapping("/{studentId}/summary")
    @Operation(summary = "Get XP Summary", description = "Returns total XP points earned by category.")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getXpSummary(@PathVariable String studentId) {
        return ResponseEntity.ok(ApiResponse.ok(xpService.getXpSummary(studentId)));
    }

    /** GET /api/v1/xp/{studentId}/history – Paginated XP history */
    @GetMapping("/{studentId}/history")
    @Operation(summary = "Get Paginated XP History", description = "Returns a paginated list of XP transactions for a student.")
    public ResponseEntity<ApiResponse<Page<XpTransaction>>> getXpHistory(
            @PathVariable String studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(xpService.getXpHistory(studentId, page, size)));
    }

    /** GET /api/v1/xp/{studentId}/streaks – Get active student streaks */
    @GetMapping("/{studentId}/streaks")
    @Operation(summary = "Get Student Streaks", description = "Returns all coding, diary, and library streaks for a student.")
    public ResponseEntity<ApiResponse<List<Streak>>> getStudentStreaks(@PathVariable String studentId) {
        return ResponseEntity.ok(ApiResponse.ok(xpService.getStudentStreaks(studentId)));
    }

    /** POST /api/v1/xp/submit – Student submits activity claim */
    @PostMapping("/submit")
    @Operation(summary = "Submit XP Claim", description = "Allows a student to submit evidence link for an activity.")
    public ResponseEntity<ApiResponse<XpTransaction>> submitXpClaim(@RequestBody ClaimSubmissionRequest request) {
        String studentId = studentAuthResolver.getLoggedInStudent().getStudentId();
        ApiResponse<XpTransaction> response = xpService.submitXpClaim(
                studentId,
                request.getCategory(),
                request.getActivityName(),
                request.getXpPoints(),
                request.getEvidenceUrl()
        );
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
                request.getStudentId(),
                request.getViolationType(),
                request.getXpPenalty(),
                appliedBy,
                request.getDescription()
        );
        return response.isSuccess() ? ResponseEntity.ok(response) : ResponseEntity.badRequest().body(response);
    }

    // Request DTOs
    public static class ClaimSubmissionRequest {
        private String category;
        private String activityName;
        private int xpPoints;
        private String evidenceUrl;

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public String getActivityName() { return activityName; }
        public void setActivityName(String activityName) { this.activityName = activityName; }

        public int getXpPoints() { return xpPoints; }
        public void setXpPoints(int xpPoints) { this.xpPoints = xpPoints; }

        public String getEvidenceUrl() { return evidenceUrl; }
        public void setEvidenceUrl(String evidenceUrl) { this.evidenceUrl = evidenceUrl; }
    }

    public static class LogViolationRequest {
        private String studentId;
        private String violationType;
        private int xpPenalty;
        private String description;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }

        public String getViolationType() { return violationType; }
        public void setViolationType(String violationType) { this.violationType = violationType; }

        public int getXpPenalty() { return xpPenalty; }
        public void setXpPenalty(int xpPenalty) { this.xpPenalty = xpPenalty; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
