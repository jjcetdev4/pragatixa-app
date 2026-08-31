package jjcet.PragatiX.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.BadgeCreateUpdateDto;
import jjcet.PragatiX.dto.BadgeDto;
import jjcet.PragatiX.modules.admin.service.AdminBadgeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/badges")
@Tag(name = "Admin Badge Controller", description = "Endpoints for managing badges (CRUD, proof requirements, recycle bin)")
public class AdminBadgeController {

    private static final Logger log = LoggerFactory.getLogger(AdminBadgeController.class);

    private final AdminBadgeService adminBadgeService;

    public AdminBadgeController(AdminBadgeService adminBadgeService) {
        this.adminBadgeService = adminBadgeService;
    }

    @GetMapping
    @Operation(summary = "Get all active badges")
    public ResponseEntity<ApiResponse<List<BadgeDto>>> getAllBadges() {
        try {
            List<BadgeDto> badges = adminBadgeService.getAllActiveBadges();
            return ResponseEntity.ok(ApiResponse.ok("Badges retrieved successfully", badges));
        } catch (Exception e) {
            log.error("Error fetching all badges", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch badges", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get badge details by ID")
    public ResponseEntity<ApiResponse<BadgeDto>> getBadgeById(@PathVariable Long id) {
        try {
            BadgeDto badge = adminBadgeService.getBadgeById(id);
            return ResponseEntity.ok(ApiResponse.ok("Badge details retrieved successfully", badge));
        } catch (Exception e) {
            log.error("Error fetching badge id {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch badge", e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Create a new badge")
    public ResponseEntity<ApiResponse<BadgeDto>> createBadge(@Valid @RequestBody BadgeCreateUpdateDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            BadgeDto created = adminBadgeService.createBadge(dto, username);
            return ResponseEntity.ok(ApiResponse.ok("Badge created successfully", created));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating badge: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating badge", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create badge", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing badge")
    public ResponseEntity<ApiResponse<BadgeDto>> updateBadge(
            @PathVariable Long id,
            @Valid @RequestBody BadgeCreateUpdateDto dto) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            BadgeDto updated = adminBadgeService.updateBadge(id, dto, username);
            return ResponseEntity.ok(ApiResponse.ok("Badge updated successfully", updated));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating badge id {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating badge id {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update badge", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete badge (move to Recycle Bin)")
    public ResponseEntity<ApiResponse<Void>> deleteBadge(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            adminBadgeService.deleteBadge(id, username);
            return ResponseEntity.ok(ApiResponse.ok("Badge moved to Recycle Bin successfully", null));
        } catch (Exception e) {
            log.error("Error deleting badge id {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete badge", e.getMessage()));
        }
    }
}
