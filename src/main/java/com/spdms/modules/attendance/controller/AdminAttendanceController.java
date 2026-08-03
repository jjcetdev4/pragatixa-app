package com.spdms.modules.attendance.controller;

import com.spdms.common.response.ApiResponse;
import com.spdms.modules.attendance.dto.response.AdminAttendanceSummaryResponse;
import com.spdms.modules.attendance.service.AdminAttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/attendance")
public class AdminAttendanceController {

    @Autowired
    private AdminAttendanceService attendanceService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminAttendanceSummaryResponse>> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Integer period,
            @RequestParam Long yearId,
            @RequestParam Long departmentId,
            @RequestParam(required = false) Long sectionId) {
            
        AdminAttendanceSummaryResponse summary = attendanceService.getDashboardSummary(date, period, yearId, departmentId, sectionId);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
