package jjcet.PragatiX.modules.admin.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.authentication.dto.request.CreateUserRequest;
import jjcet.PragatiX.modules.authentication.dto.request.UpdateUserRequest;
import jjcet.PragatiX.modules.authentication.dto.response.UserResponse;
import jjcet.PragatiX.entity.User;
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
import jjcet.PragatiX.modules.admin.service.*;
import jjcet.PragatiX.modules.admin.mapper.*;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "AdminUserController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {
    private static final Logger log = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;
    private final AdminBulkTeacherService adminBulkTeacherService;

    public AdminUserController(AdminUserService adminUserService, AdminBulkTeacherService adminBulkTeacherService) {
        this.adminUserService = adminUserService;
        this.adminBulkTeacherService = adminBulkTeacherService;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERADMIN', 'TEACHER', 'CLASS_COORDINATOR')")
    @Operation(summary = "List All Users", description = "Returns all staff/users (teachers and admins) with optional department and keyword filters.")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String keyword) {
        return adminUserService.getAllUsers(departmentId, keyword);
    }

    @GetMapping("/users/{id}/points-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERADMIN', 'TEACHER', 'CLASS_COORDINATOR')")
    @Operation(summary = "Get Teacher Points History", description = "Returns profile and detailed history of all points awarded by a teacher.")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getTeacherPointsHistory(@PathVariable Long id) {
        return adminUserService.getTeacherPointsHistory(id);
    }

    @PostMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Create User", description = "Creates a new teacher or admin account.")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        return adminUserService.createUser(request);
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Update User", description = "Updates teacher or admin profile information and role selections.")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return adminUserService.updateUser(id, request);
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SUPERADMIN')")
    @Operation(summary = "Delete User", description = "Deletes a teacher or admin staff account. Requires ADMIN role.")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        return adminUserService.deleteUser(id);
    }

    @GetMapping("/users/bulk-upload/template")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Download Teacher Bulk Upload Template", description = "Generates and downloads an Excel template for bulk teacher upload.")
    public ResponseEntity<?> downloadBulkUploadTemplate() {
        try {
            byte[] templateBytes = adminBulkTeacherService.generateTemplate();
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "SPDMS_Teacher_Bulk_Upload_Template.xlsx");
            return new ResponseEntity<>(templateBytes, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to generate bulk upload template: {}", e.getMessage(), e);
            // Return safe error to frontend to avoid leaking internal POI/Hibernate info
            return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Unable to generate the teacher upload template."));
        }
    }

    @PostMapping(value = "/users/bulk-parse", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk Parse Teachers Spreadsheet", description = "Parses Excel and returns JSON list of valid teachers and any errors.")
    public ResponseEntity<ApiResponse<List<CreateUserRequest>>> bulkParseTeachers(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        ApiResponse<List<CreateUserRequest>> response = adminBulkTeacherService.bulkParse(file);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/users/bulk-import")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk Import Selected Teachers", description = "Saves selected list of parsed teacher records into the database.")
    public ResponseEntity<ApiResponse<String>> bulkImportTeachers(
            @RequestBody List<CreateUserRequest> requests) {
        ApiResponse<String> response = adminBulkTeacherService.bulkImport(requests);
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

}
