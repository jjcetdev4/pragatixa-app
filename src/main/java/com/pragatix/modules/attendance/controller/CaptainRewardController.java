package com.pragatix.modules.attendance.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.attendance.dto.CaptainRewardSettingsDTO;
import com.pragatix.modules.attendance.service.CaptainRewardSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/captain-reward")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class CaptainRewardController {

    @Autowired
    private CaptainRewardSettingsService settingsService;

    @GetMapping("/settings/{year}")
    public ResponseEntity<ApiResponse<CaptainRewardSettingsDTO>> getSettings(@PathVariable AcademicYear year) {
        CaptainRewardSettingsDTO settings = settingsService.getSettings(year);
        return ResponseEntity.ok(ApiResponse.ok("Fetched settings", settings));
    }

    @PutMapping("/settings/{year}")
    public ResponseEntity<ApiResponse<CaptainRewardSettingsDTO>> updateSettings(
            @PathVariable AcademicYear year,
            @RequestBody CaptainRewardSettingsDTO request) {
        CaptainRewardSettingsDTO updated = settingsService.updateSettings(year, request);
        return ResponseEntity.ok(ApiResponse.ok("Updated settings", updated));
    }
}
