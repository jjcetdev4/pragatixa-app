package jjcet.PragatiX.modules.recyclebin.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.recyclebin.dto.RecycleBinItem;
import jjcet.PragatiX.modules.recyclebin.service.RecycleBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recycle-bin")
@Tag(name = "Recycle Bin Controller", description = "Endpoints for managing soft-deleted records")
@SecurityRequirement(name = "bearerAuth")
public class RecycleBinController {

    private final RecycleBinService recycleBinService;

    public RecycleBinController(RecycleBinService recycleBinService) {
        this.recycleBinService = recycleBinService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Get all items in the Recycle Bin")
    public ResponseEntity<ApiResponse<List<RecycleBinItem>>> getDeletedItems() {
        List<RecycleBinItem> items = recycleBinService.getDeletedItems();
        return ResponseEntity.ok(ApiResponse.ok("Recycle bin items retrieved successfully", items));
    }

    @PostMapping("/restore/{entityType}/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Restore a soft-deleted item")
    public ResponseEntity<ApiResponse<Void>> restoreItem(@PathVariable String entityType, @PathVariable Long id) {
        recycleBinService.restoreItem(entityType, id);
        return ResponseEntity.ok(ApiResponse.ok(entityType + " restored successfully", null));
    }

    @DeleteMapping("/permanent/{entityType}/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Permanently delete an item")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteItem(@PathVariable String entityType, @PathVariable Long id) {
        try {
            recycleBinService.permanentlyDeleteItem(entityType, id);
            return ResponseEntity.ok(ApiResponse.ok("Item permanently deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete permanently: " + e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Clear/Empty the entire Recycle Bin")
    public ResponseEntity<ApiResponse<Void>> clearRecycleBin() {
        try {
            int clearedCount = recycleBinService.clearAllItems();
            return ResponseEntity.ok(ApiResponse.ok("Recycle bin emptied successfully (" + clearedCount + " items removed)", null));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to clear recycle bin: " + e.getMessage()));
        }
    }
}
