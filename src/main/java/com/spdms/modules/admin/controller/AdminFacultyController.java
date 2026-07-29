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
@Tag(name = "AdminFacultyController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminFacultyController {
    private static final Logger log = LoggerFactory.getLogger(AdminFacultyController.class);

    private final AdminFacultyService adminFacultyService;

    public AdminFacultyController(AdminFacultyService adminFacultyService) {
        this.adminFacultyService = adminFacultyService;
    }

    @PutMapping("/subgroups/{id}/assign-faculty")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign a faculty member to an activity subgroup")
    public ResponseEntity<ApiResponse<ActivitySubgroup>> assignFacultyToSubgroup(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        return adminFacultyService.assignFacultyToSubgroup(id, body);
    }

}
