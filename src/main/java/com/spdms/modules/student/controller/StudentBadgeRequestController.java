package com.pragatix.modules.student.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.dto.BadgeRequestCreateDto;
import com.pragatix.dto.BadgeRequestDto;
import com.pragatix.modules.badge.service.BadgeRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badge-requests")
public class StudentBadgeRequestController {

    private static final Logger log = LoggerFactory.getLogger(StudentBadgeRequestController.class);

    private final BadgeRequestService badgeRequestService;

    public StudentBadgeRequestController(BadgeRequestService badgeRequestService) {
        this.badgeRequestService = badgeRequestService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BadgeRequestDto>> requestBadge(@Valid @RequestBody BadgeRequestCreateDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            BadgeRequestDto result = badgeRequestService.createRequest(dto, username);
            return ResponseEntity.ok(ApiResponse.ok("Badge request submitted successfully", result));
        } catch (Exception e) {
            log.error("Error submitting badge request", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to submit badge request", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<BadgeRequestDto>>> getMyRequests() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            List<BadgeRequestDto> results = badgeRequestService.getMyRequests(username);
            return ResponseEntity.ok(ApiResponse.ok("Fetched requests successfully", results));
        } catch (Exception e) {
            log.error("Error fetching my badge requests", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch requests", e.getMessage()));
        }
    }
}
