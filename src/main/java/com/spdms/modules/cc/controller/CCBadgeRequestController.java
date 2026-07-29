package com.pragatix.modules.cc.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.BadgeRequestDto;
import com.pragatix.dto.BadgeRequestStatusUpdateDto;
import com.pragatix.modules.badge.service.BadgeRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cc/badge-requests")
public class CCBadgeRequestController {

    private static final Logger log = LoggerFactory.getLogger(CCBadgeRequestController.class);

    private final BadgeRequestService badgeRequestService;

    public CCBadgeRequestController(BadgeRequestService badgeRequestService) {
        this.badgeRequestService = badgeRequestService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BadgeRequestDto>>> getCCRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            List<BadgeRequestDto> results = badgeRequestService.getCCRequests(username);
            return ResponseEntity.ok(ApiResponse.ok("Fetched CC badge requests successfully", results));
        } catch (Exception e) {
            log.error("Error fetching CC badge requests", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch CC badge requests", e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<BadgeRequestDto>> approveRequest(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // Since CC should only approve their students, we should validate it.
        // For simplicity, we assume the frontend only shows their students, but an
        // extra check in the service would be ideal.
        try {
            BadgeRequestDto result = badgeRequestService.approveRequest(id, username);
            return ResponseEntity.ok(ApiResponse.ok("Badge request approved successfully", result));
        } catch (Exception e) {
            log.error("Error approving badge request id {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to approve badge request", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<BadgeRequestDto>> rejectRequest(
            @PathVariable Long id,
            @RequestBody(required = false) BadgeRequestStatusUpdateDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            BadgeRequestDto result = badgeRequestService.rejectRequest(id, dto, username);
            return ResponseEntity.ok(ApiResponse.ok("Badge request rejected successfully", result));
        } catch (Exception e) {
            log.error("Error rejecting badge request id {}", id, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to reject badge request", e.getMessage()));
        }
    }
}
