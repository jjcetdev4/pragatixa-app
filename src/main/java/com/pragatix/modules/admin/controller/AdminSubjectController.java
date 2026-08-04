package com.pragatix.modules.admin.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Subject;
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
@Tag(name = "AdminSubjectController", description = "Admin endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminSubjectController {
    private static final Logger log = LoggerFactory.getLogger(AdminSubjectController.class);

    private final AdminSubjectService adminSubjectService;

    public AdminSubjectController(AdminSubjectService adminSubjectService) {
        this.adminSubjectService = adminSubjectService;
    }

    @GetMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all dynamic subjects")
    public ResponseEntity<ApiResponse<List<Subject>>> getAllSubjects() {
        return adminSubjectService.getAllSubjects();
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a dynamic subject")
    public ResponseEntity<ApiResponse<Subject>> createSubject(@RequestBody Map<String, String> body) {
        return adminSubjectService.createSubject(body);
    }

    @DeleteMapping("/subjects/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a dynamic subject")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        return adminSubjectService.deleteSubject(id);
    }

}
