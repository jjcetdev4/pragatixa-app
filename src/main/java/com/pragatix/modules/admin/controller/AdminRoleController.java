package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminRoleController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminRoleController {
    private static final Logger log = LoggerFactory.getLogger(AdminRoleController.class);

    private final AdminRoleService adminRoleService;

    public AdminRoleController(AdminRoleService adminRoleService) {
        this.adminRoleService = adminRoleService;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List Roles")
    public ResponseEntity<ApiResponse<List<Role>>> getAllRoles() {
        return adminRoleService.getAllRoles();
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create Role")
    public ResponseEntity<ApiResponse<Role>> createRole(@RequestBody Map<String, String> body) {
        return adminRoleService.createRole(body);
    }

}
