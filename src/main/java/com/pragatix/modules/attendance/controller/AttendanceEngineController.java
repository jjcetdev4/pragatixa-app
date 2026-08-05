package com.pragatix.modules.attendance.controller;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.enums.AcademicYear;
import com.pragatix.modules.attendance.service.AttendanceDailyEngineService;
import com.pragatix.modules.attendance.service.AttendanceWeeklyEngineService;
import com.pragatix.modules.attendancesettings.dto.AttendanceSettingsDto;
import com.pragatix.modules.attendancesettings.repository.AttendanceSettingsRepository;
import com.pragatix.modules.attendancesettings.service.AttendanceSettingsService;
import com.pragatix.modules.attendancesettings.service.EngineClockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * AttendanceEngineController - REST API for the Attendance Engine Control Center.
 *
 * Provides endpoints to:
 * - Get engine status for an Academic Year
 * - Manually run the Daily Engine
 * - Manually run the Weekly Engine
 * - Run both engines sequentially
 * - Reset the engine state (clears status flags, no data deleted)
 */
@RestController
@RequestMapping("/api/v1/attendance-engine")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ROLE_ADMIN')")
public class AttendanceEngineController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceEngineController.class);

    @Autowired private AttendanceDailyEngineService dailyEngineService;
    @Autowired private AttendanceWeeklyEngineService weeklyEngineService;
    @Autowired private AttendanceSettingsService settingsService;
    @Autowired private AttendanceSettingsRepository settingsRepository;
    @Autowired private EngineClockService clockService;

    /**
     * GET /api/v1/attendance-engine/status?academicYear=SECOND_YEAR
     * Returns the current engine status and settings for the given Academic Year.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AttendanceSettingsDto>> getStatus(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null) academicYear = AcademicYear.FIRST_YEAR;
        AttendanceSettingsDto dto = settingsService.getSettings(academicYear);
        return ResponseEntity.ok(ApiResponse.ok("Engine status retrieved", dto));
    }

    /**
     * POST /api/v1/attendance-engine/run-daily?academicYear=SECOND_YEAR
     * Manually triggers the Daily Attendance Engine using the effective date (test or production).
     */
    @PostMapping("/run-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runDaily(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null) academicYear = AcademicYear.FIRST_YEAR;
        log.info("[TRACE] ======================================================");
        log.info("[TRACE] |  MANUAL TRIGGER: Daily Engine via REST API          |");
        log.info("[TRACE] ======================================================");
        log.info("[TRACE] Academic Year = {}", academicYear);
        log.info("Manual trigger: Daily Engine for {}", academicYear);
        Map<String, Object> result = dailyEngineService.execute(academicYear);
        log.info("[TRACE] MANUAL Daily Engine returned: {}", result);
        return ResponseEntity.ok(ApiResponse.ok("Daily engine executed", result));
    }

    /**
     * POST /api/v1/attendance-engine/run-weekly?academicYear=SECOND_YEAR
     * Manually triggers the Weekly Attendance Engine.
     */
    @PostMapping("/run-weekly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runWeekly(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null) academicYear = AcademicYear.FIRST_YEAR;
        log.info("[TRACE] ======================================================");
        log.info("[TRACE] |  MANUAL TRIGGER: Weekly Engine via REST API         |");
        log.info("[TRACE] ======================================================");
        log.info("[TRACE] Academic Year = {}", academicYear);
        log.info("Manual trigger: Weekly Engine for {}", academicYear);
        Map<String, Object> result = weeklyEngineService.execute(academicYear);
        log.info("[TRACE] MANUAL Weekly Engine returned: {}", result);
        return ResponseEntity.ok(ApiResponse.ok("Weekly engine executed", result));
    }

    /**
     * POST /api/v1/attendance-engine/run-both?academicYear=SECOND_YEAR
     * Runs Daily Engine first, then Weekly Engine.
     */
    @PostMapping("/run-both")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runBoth(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null) academicYear = AcademicYear.FIRST_YEAR;
        log.info("Manual trigger: Both Engines for {}", academicYear);
        Map<String, Object> dailyResult = dailyEngineService.execute(academicYear);
        Map<String, Object> weeklyResult = weeklyEngineService.execute(academicYear);
        Map<String, Object> combined = Map.of(
            "daily", dailyResult,
            "weekly", weeklyResult
        );
        return ResponseEntity.ok(ApiResponse.ok("Both engines executed", combined));
    }

    /**
     * POST /api/v1/attendance-engine/reset?academicYear=SECOND_YEAR
     * Resets engine status flags only. Does NOT delete attendance records or XP.
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<AttendanceSettingsDto>> resetState(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null) academicYear = AcademicYear.FIRST_YEAR;
        final AcademicYear finalYear = academicYear;
        log.info("Engine state reset for {}", academicYear);

        settingsRepository.findByAcademicYear(academicYear).ifPresent(settings -> {
            settings.setDailyEngineStatus("WAITING");
            settings.setWeeklyEngineStatus("WAITING");
            settings.setLastDailyRun(null);
            settings.setLastWeeklyRun(null);
            settingsRepository.save(settings);
        });

        AttendanceSettingsDto dto = settingsService.getSettings(finalYear);
        return ResponseEntity.ok(ApiResponse.ok("Engine state reset successfully", dto));
    }
}
