package jjcet.PragatiX.modules.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.dto.ActivityCategoryCreateUpdateDto;
import jjcet.PragatiX.dto.ActivityCategoryDto;
import jjcet.PragatiX.modules.admin.service.ActivityCategoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Tag(name = "ActivityCategoryController", description = "Activity & XP Category Management Endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ActivityCategoryController {

    private static final Logger log = LoggerFactory.getLogger(ActivityCategoryController.class);

    private final ActivityCategoryService categoryService;

    public ActivityCategoryController(ActivityCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/v1/categories")
    @Operation(summary = "Get all active activity categories (for dropdowns)")
    public ResponseEntity<ApiResponse<List<ActivityCategoryDto>>> getPublicCategories() {
        return ResponseEntity.ok(ApiResponse.ok("Categories fetched successfully", categoryService.getAllActiveCategories()));
    }

    @GetMapping("/api/v1/categories/names")
    @Operation(summary = "Get all active category names as a simple list")
    public ResponseEntity<ApiResponse<List<String>>> getCategoryNames() {
        return ResponseEntity.ok(ApiResponse.ok("Category names fetched successfully", categoryService.getActiveCategoryNames()));
    }

    @GetMapping("/api/v1/admin/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get all active categories for admin management")
    public ResponseEntity<ApiResponse<List<ActivityCategoryDto>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok("Admin categories fetched successfully", categoryService.getAllActiveCategories()));
    }

    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create a new activity category")
    public ResponseEntity<ApiResponse<ActivityCategoryDto>> createCategory(
            @Valid @RequestBody ActivityCategoryCreateUpdateDto dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        ActivityCategoryDto created = categoryService.createCategory(dto, username);
        return ResponseEntity.ok(ApiResponse.ok("Category created successfully", created));
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update an existing activity category")
    public ResponseEntity<ApiResponse<ActivityCategoryDto>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ActivityCategoryCreateUpdateDto dto,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        ActivityCategoryDto updated = categoryService.updateCategory(id, dto, username);
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", updated));
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Soft delete an activity category (moves to Recycle Bin)")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "admin";
        categoryService.deleteCategory(id, username);
        return ResponseEntity.ok(ApiResponse.ok("Category moved to Recycle Bin successfully", null));
    }
}
