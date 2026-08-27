package jjcet.PragatiX.modules.attendance.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.entity.AttendanceEngineExecution;
import jjcet.PragatiX.enums.AcademicYear;
import jjcet.PragatiX.modules.attendance.repository.AttendanceEngineExecutionRepository;
import jjcet.PragatiX.modules.attendance.service.AttendanceDailyEngineService;
import jjcet.PragatiX.modules.attendance.service.AttendanceWeeklyEngineService;
import jjcet.PragatiX.modules.attendancesettings.dto.AttendanceSettingsDto;
import jjcet.PragatiX.modules.attendancesettings.repository.AttendanceSettingsRepository;
import jjcet.PragatiX.modules.attendancesettings.service.AttendanceSettingsService;
import jjcet.PragatiX.modules.attendancesettings.service.EngineClockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * AttendanceEngineController - REST API for the Attendance Engine Control Center.
 *
 * Provides endpoints to:
 * - Get engine status for an Academic Year
 * - Manually run the Daily Engine
 * - Manually run the Weekly Engine
 * - Run both engines sequentially
 * - View execution history logs
 * - Reset the engine state
 */
@RestController
@RequestMapping("/api/v1/attendance-engine")
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ROLE_ADMIN')")
public class AttendanceEngineController {

    private static final Logger log = LoggerFactory.getLogger(AttendanceEngineController.class);

    @Autowired
    private AttendanceDailyEngineService dailyEngineService;
    @Autowired
    private AttendanceWeeklyEngineService weeklyEngineService;
    @Autowired
    private AttendanceSettingsService settingsService;
    @Autowired
    private AttendanceSettingsRepository settingsRepository;
    @Autowired
    private EngineClockService clockService;
    @Autowired
    private AttendanceEngineExecutionRepository executionRepository;

    /**
     * GET /api/v1/attendance-engine/status?academicYear=SECOND_YEAR
     * Returns the current engine status and settings for the given Academic Year.
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<AttendanceSettingsDto>> getStatus(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null)
            academicYear = AcademicYear.FIRST_YEAR;
        AttendanceSettingsDto dto = settingsService.getSettings(academicYear);
        return ResponseEntity.ok(ApiResponse.ok("Engine status retrieved", dto));
    }

    /**
     * POST /api/v1/attendance-engine/run-daily?academicYear=SECOND_YEAR&date=2026-08-27
     * Manually triggers the Daily Attendance Engine.
     */
    @PostMapping("/run-daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runDaily(
            @RequestParam(required = false) AcademicYear academicYear,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (academicYear == null)
            academicYear = AcademicYear.FIRST_YEAR;

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "ADMIN";

        log.info("[MANUAL TRIGGER] Daily Engine for {} on {} by {}", academicYear, date, username);
        Map<String, Object> result = dailyEngineService.execute(academicYear, date, "MANUAL", username);
        return ResponseEntity.ok(ApiResponse.ok("Daily engine executed", result));
    }

    /**
     * POST /api/v1/attendance-engine/run-weekly?academicYear=SECOND_YEAR&startDate=2026-08-25&endDate=2026-08-31
     * Manually triggers the Weekly Attendance Engine.
     */
    @PostMapping("/run-weekly")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runWeekly(
            @RequestParam(required = false) AcademicYear academicYear,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        if (academicYear == null)
            academicYear = AcademicYear.FIRST_YEAR;

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "ADMIN";

        log.info("[MANUAL TRIGGER] Weekly Engine for {} ({} to {}) by {}", academicYear, startDate, endDate, username);
        Map<String, Object> result = weeklyEngineService.execute(academicYear, startDate, endDate, "MANUAL", username);
        return ResponseEntity.ok(ApiResponse.ok("Weekly engine executed", result));
    }

    /**
     * POST /api/v1/attendance-engine/run-both?academicYear=SECOND_YEAR
     * Runs Daily Engine first, then Weekly Engine.
     */
    @PostMapping("/run-both")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runBoth(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null)
            academicYear = AcademicYear.FIRST_YEAR;

        String username = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : "ADMIN";

        log.info("[MANUAL TRIGGER] Both Engines for {} by {}", academicYear, username);
        Map<String, Object> dailyResult = dailyEngineService.execute(academicYear, null, "MANUAL", username);
        Map<String, Object> weeklyResult = weeklyEngineService.execute(academicYear, null, null, "MANUAL", username);
        Map<String, Object> combined = Map.of(
                "daily", dailyResult,
                "weekly", weeklyResult);
        return ResponseEntity.ok(ApiResponse.ok("Both engines executed", combined));
    }

    /**
     * GET /api/v1/attendance-engine/history?academicYear=SECOND_YEAR
     * Returns recent execution history for the given Academic Year.
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<AttendanceEngineExecution>>> getHistory(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null)
            academicYear = AcademicYear.FIRST_YEAR;
        List<AttendanceEngineExecution> history = executionRepository.findTop20ByAcademicYearOrderByStartedAtDesc(academicYear);
        return ResponseEntity.ok(ApiResponse.ok("Execution history retrieved", history));
    }

    /**
     * POST /api/v1/attendance-engine/reset?academicYear=SECOND_YEAR
     * Resets engine status flags only. Does NOT delete attendance records or XP.
     */
    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<AttendanceSettingsDto>> resetState(
            @RequestParam(required = false) AcademicYear academicYear) {
        if (academicYear == null)
            academicYear = AcademicYear.FIRST_YEAR;
        final AcademicYear finalYear = academicYear;
        log.info("Engine state reset for {}", academicYear);

        settingsRepository.findByAcademicYear(academicYear).ifPresent(settings -> {
            settings.setDailyEngineStatus("WAITING");
            settings.setWeeklyEngineStatus("WAITING");
            settings.setLastDailyRun(null);
            settings.setLastDailyRunStatus("WAITING");
            settings.setLastWeeklyRun(null);
            settings.setLastWeeklyRunStatus("WAITING");
            settingsRepository.save(settings);
        });

        AttendanceSettingsDto dto = settingsService.getSettings(finalYear);
        return ResponseEntity.ok(ApiResponse.ok("Engine state reset successfully", dto));
    }
}
