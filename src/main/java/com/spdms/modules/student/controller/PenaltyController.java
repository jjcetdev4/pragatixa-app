package com.pragatix.modules.student.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.student.dto.request.CreatePenaltyRequestDto;
import com.pragatix.modules.student.dto.response.PenaltyRequestDto;
import com.pragatix.modules.student.service.PenaltyWorkflowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/penalties")
public class PenaltyController {

    private final PenaltyWorkflowService penaltyWorkflowService;

    public PenaltyController(PenaltyWorkflowService penaltyWorkflowService) {
        this.penaltyWorkflowService = penaltyWorkflowService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PenaltyRequestDto>> submitPenalty(
            @RequestBody CreatePenaltyRequestDto dto,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(penaltyWorkflowService.submitPenalty(dto, username));
    }

    @GetMapping("/cc-inbox")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PenaltyRequestDto>>> getCcInbox(
            @RequestParam(required = false) String status,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(penaltyWorkflowService.getCcInbox(username, status));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<PenaltyRequestDto>>> getMyRequests(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(penaltyWorkflowService.getMyRequests(username));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PenaltyRequestDto>> approvePenalty(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(penaltyWorkflowService.approvePenalty(id, username));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<PenaltyRequestDto>> rejectPenalty(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication authentication) {
        String username = authentication.getName();
        String reason = body.getOrDefault("reason", "Rejected by CC");
        return ResponseEntity.ok(penaltyWorkflowService.rejectPenalty(id, username, reason));
    }
}
