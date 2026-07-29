package com.pragatix.modules.student.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.student.dto.request.CreateActivityCompletionRequestDto;
import com.pragatix.modules.student.dto.response.ActivityCompletionRequestDto;
import com.pragatix.modules.student.service.ActivityCompletionRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activity-requests")
public class ActivityCompletionRequestController {

    private final ActivityCompletionRequestService service;

    public ActivityCompletionRequestController(ActivityCompletionRequestService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<ActivityCompletionRequestDto>> submitRequest(
            @RequestBody CreateActivityCompletionRequestDto dto,
            Authentication authentication) {
        String username = authentication.getName(); // For student, this is regNo
        return ResponseEntity.ok(service.submitRequest(dto, username));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<ActivityCompletionRequestDto>>> getMyRequests(
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(service.getStudentRequests(username));
    }

    @GetMapping("/inbox")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'CLASS_COORDINATOR')")
    public ResponseEntity<ApiResponse<List<ActivityCompletionRequestDto>>> getInbox(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        String username = authentication.getName();

        System.out.println("----- Entered ActivityCompletionRequestController.getInbox() -----");
        System.out.println("Logged-in Username: " + username);
        System.out.println("Roles: " + authentication.getAuthorities());

        return ResponseEntity.ok(service.getInbox(username, status));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'CLASS_COORDINATOR')")
    public ResponseEntity<ApiResponse<ActivityCompletionRequestDto>> approveRequest(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(service.approveRequest(id, username));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN', 'CLASS_COORDINATOR')")
    public ResponseEntity<ApiResponse<ActivityCompletionRequestDto>> rejectRequest(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String username = authentication.getName();
        String reason = body.getOrDefault("reason", "Rejected by teacher");
        return ResponseEntity.ok(service.rejectRequest(id, username, reason));
    }
}
