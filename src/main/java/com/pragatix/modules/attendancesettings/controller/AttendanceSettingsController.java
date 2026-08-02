package com.pragatix.modules.attendancesettings.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.modules.attendancesettings.dto.AttendanceSettingsDto;
import com.pragatix.modules.attendancesettings.service.AttendanceSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.authentication.security.AuthUtils;
import com.pragatix.entity.User;
import java.util.List;

@RestController
@RequestMapping("/api/v1/attendance-settings")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ROLE_ADMIN')")
public class AttendanceSettingsController {

    @Autowired
    private AttendanceSettingsService settingsService;

    @Autowired
    private AuthUtils authUtils;

    // --- Settings ---

    @GetMapping
    public ResponseEntity<ApiResponse<AttendanceSettingsDto>> getSettings(@RequestParam(required = false) AcademicYear academicYear) {
        User currentUser = authUtils.getCurrentUser();
        if (academicYear == null && authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
            academicYear = currentUser.getAcademicYear();
        }
        AttendanceSettingsDto settings = settingsService.getSettings(academicYear);
        return ResponseEntity.ok(ApiResponse.ok("Settings retrieved successfully", settings));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AttendanceSettingsDto>> updateSettings(@RequestBody AttendanceSettingsDto dto, @RequestParam(required = false) AcademicYear academicYear) {
        User currentUser = authUtils.getCurrentUser();
        if (academicYear == null && authUtils.isAdmin(currentUser) && !authUtils.isSuperAdmin(currentUser)) {
            academicYear = currentUser.getAcademicYear();
        }
        AttendanceSettingsDto updated = settingsService.updateSettings(dto, academicYear);
        return ResponseEntity.ok(ApiResponse.ok("Settings updated successfully", updated));
    }

}
