package jjcet.PragatiX.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.admin.dto.request.LevelCreateUpdateDto;
import jjcet.PragatiX.modules.admin.dto.response.LevelDto;
import jjcet.PragatiX.modules.admin.service.AdminLevelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/levels")
@Tag(name = "Admin Level Controller", description = "Endpoints for managing Year-wise XP Levels (CRUD, objectives, unlocks, recycle bin)")
public class AdminLevelController {

    private static final Logger log = LoggerFactory.getLogger(AdminLevelController.class);

    private final AdminLevelService adminLevelService;

    public AdminLevelController(AdminLevelService adminLevelService) {
        this.adminLevelService = adminLevelService;
    }

    @GetMapping
    @Operation(summary = "Get levels filtered by academic year (or all for Super Admin)")
    public ResponseEntity<ApiResponse<List<LevelDto>>> getLevels(
            @RequestParam(required = false) String academicYear) {
        try {
            List<LevelDto> levels = adminLevelService.getLevels(academicYear);
            return ResponseEntity.ok(ApiResponse.ok("Levels retrieved successfully", levels));
        } catch (Exception e) {
            log.error("Error fetching levels", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch levels: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get level details by ID")
    public ResponseEntity<ApiResponse<LevelDto>> getLevelById(@PathVariable Long id) {
        try {
            LevelDto level = adminLevelService.getLevel(id);
            return ResponseEntity.ok(ApiResponse.ok("Level details retrieved successfully", level));
        } catch (Exception e) {
            log.error("Error fetching level id {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to fetch level: " + e.getMessage()));
        }
    }

    @PostMapping
    @Operation(summary = "Create a new Level")
    public ResponseEntity<ApiResponse<LevelDto>> createLevel(@Valid @RequestBody LevelCreateUpdateDto dto) {
        try {
            LevelDto created = adminLevelService.createLevel(dto);
            return ResponseEntity.ok(ApiResponse.ok("Level created successfully", created));
        } catch (Exception e) {
            log.error("Error creating level", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to create level: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing Level")
    public ResponseEntity<ApiResponse<LevelDto>> updateLevel(
            @PathVariable Long id,
            @Valid @RequestBody LevelCreateUpdateDto dto) {
        try {
            LevelDto updated = adminLevelService.updateLevel(id, dto);
            return ResponseEntity.ok(ApiResponse.ok("Level updated successfully", updated));
        } catch (Exception e) {
            log.error("Error updating level id {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to update level: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete Level to Recycle Bin")
    public ResponseEntity<ApiResponse<Void>> deleteLevel(@PathVariable Long id) {
        try {
            adminLevelService.deleteLevel(id);
            return ResponseEntity.ok(ApiResponse.ok("Level moved to recycle bin successfully", null));
        } catch (Exception e) {
            log.error("Error deleting level id {}", id, e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to delete level: " + e.getMessage()));
        }
    }
}
