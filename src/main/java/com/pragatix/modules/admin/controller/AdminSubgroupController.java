package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.ActivitySubgroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import org.springframework.web.bind.annotation.RestController;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminSubgroupController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminSubgroupController {
    private static final Logger log = LoggerFactory.getLogger(AdminSubgroupController.class);

    private final AdminSubgroupService adminSubgroupService;

    public AdminSubgroupController(AdminSubgroupService adminSubgroupService) {
        this.adminSubgroupService = adminSubgroupService;
    }

    @PostMapping("/stages/{stageId}/subgroups")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a subgroup under a stage")
    public ResponseEntity<ApiResponse<ActivitySubgroup>> createSubgroup(
            @PathVariable Long stageId,
            @RequestBody Map<String, Object> body) {
        return adminSubgroupService.createSubgroup(stageId, body);
    }

    @PutMapping("/subgroups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a subgroup's name or threshold")
    public ResponseEntity<ApiResponse<ActivitySubgroup>> updateSubgroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return adminSubgroupService.updateSubgroup(id, body);
    }

    @DeleteMapping("/subgroups/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a subgroup")
    public ResponseEntity<ApiResponse<Void>> deleteSubgroup(@PathVariable Long id) {
        return adminSubgroupService.deleteSubgroup(id);
    }

}
