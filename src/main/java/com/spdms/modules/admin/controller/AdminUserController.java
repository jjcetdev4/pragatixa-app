package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.authentication.dto.request.CreateUserRequest;
import com.pragatix.modules.authentication.dto.request.UpdateUserRequest;
import com.pragatix.modules.authentication.dto.response.UserResponse;
import com.pragatix.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;
import com.pragatix.modules.admin.service.*;
import com.pragatix.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminUserController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Operation(summary = "List All Users", description = "Returns all staff/users (teachers and admins).")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return adminUserService.getAllUsers();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create User", description = "Creates a new teacher or admin account.")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return adminUserService.createUser(request);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update User", description = "Updates teacher or admin profile information and role selections.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return adminUserService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete User", description = "Deletes a teacher or admin staff account. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        return adminUserService.deleteUser(id);
    }

}
