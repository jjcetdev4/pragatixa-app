package jjcet.PragatiX.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.ActivityEvidenceCreateUpdateDto;
import jjcet.PragatiX.dto.ActivityEvidenceDto;
import jjcet.PragatiX.modules.admin.service.ActivityEvidenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "ActivityEvidenceController", description = "Activity Evidence Types Management Endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ActivityEvidenceController {

    private static final Logger log = LoggerFactory.getLogger(ActivityEvidenceController.class);

    private final ActivityEvidenceService evidenceService;

    public ActivityEvidenceController(ActivityEvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    @GetMapping("/api/v1/evidence")
    @Operation(summary = "Get all active evidence types (for selectors and forms)")
    public ResponseEntity<ApiResponse<List<ActivityEvidenceDto>>> getPublicEvidences() {
        return ResponseEntity.ok(ApiResponse.ok("Evidence types fetched successfully", evidenceService.getAllActiveEvidences()));
    }

    @GetMapping("/api/v1/evidence/names")
    @Operation(summary = "Get all active evidence names as a list of strings")
    public ResponseEntity<ApiResponse<List<String>>> getEvidenceNames() {
        return ResponseEntity.ok(ApiResponse.ok("Evidence names fetched successfully", evidenceService.getAllActiveEvidenceNames()));
    }

    @GetMapping("/api/v1/admin/evidence")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all active evidence types for admin management")
    public ResponseEntity<ApiResponse<List<ActivityEvidenceDto>>> getAllAdminEvidences() {
        return ResponseEntity.ok(ApiResponse.ok("Admin evidence types fetched successfully", evidenceService.getAllActiveEvidences()));
    }

    @PostMapping("/api/v1/admin/evidence")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new activity evidence type")
    public ResponseEntity<ApiResponse<ActivityEvidenceDto>> createEvidence(
            @Valid @RequestBody ActivityEvidenceCreateUpdateDto dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        ActivityEvidenceDto created = evidenceService.createEvidence(dto, username);
        return ResponseEntity.ok(ApiResponse.ok("Evidence type created successfully", created));
    }

    @PutMapping("/api/v1/admin/evidence/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update an existing activity evidence type")
    public ResponseEntity<ApiResponse<ActivityEvidenceDto>> updateEvidence(
            @PathVariable Long id,
            @Valid @RequestBody ActivityEvidenceCreateUpdateDto dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        ActivityEvidenceDto updated = evidenceService.updateEvidence(id, dto, username);
        return ResponseEntity.ok(ApiResponse.ok("Evidence type updated successfully", updated));
    }

    @DeleteMapping("/api/v1/admin/evidence/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Soft delete an activity evidence type (moves to Recycle Bin)")
    public ResponseEntity<ApiResponse<String>> deleteEvidence(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        evidenceService.deleteEvidence(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Evidence type moved to Recycle Bin successfully", null));
    }
}
