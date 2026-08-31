package jjcet.PragatiX.modules.analytics.controller;

import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.service.AnalyticsService;
import jjcet.PragatiX.modules.analytics.scoped.service.AnalyticsScopeResolver;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Legacy controller superseded by api.controller.AnalyticsController
// @RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_SUPER_ADMIN', 'ROLE_SUPERADMIN', 'ADMIN', 'SUPER_ADMIN', 'SUPERADMIN', 'ROLE_HOD', 'HOD', 'ROLE_FACULTY', 'FACULTY', 'ROLE_STUDENT', 'STUDENT')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final AnalyticsScopeResolver scopeResolver;
    private final AuthUtils authUtils;

    public AnalyticsController(AnalyticsService analyticsService,
                               AnalyticsScopeResolver scopeResolver,
                               AuthUtils authUtils) {
        this.analyticsService = analyticsService;
        this.scopeResolver = scopeResolver;
        this.authUtils = authUtils;
    }

    @GetMapping("/institution-growth")
    public ResponseEntity<ApiResponse<List<InstitutionGrowthDto>>> getInstitutionGrowth(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<InstitutionGrowthDto> data = analyticsService.getInstitutionGrowth(yearNo, semester, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/department-growth")
    public ResponseEntity<ApiResponse<List<DepartmentGrowthDto>>> getDepartmentGrowth(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<DepartmentGrowthDto> data = analyticsService.getDepartmentGrowth(yearNo, semester, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/xp-curve")
    public ResponseEntity<ApiResponse<List<XpCurveDto>>> getXpCurve(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        User currentUser = authUtils.getCurrentUser();
        scopeResolver.validateDepartmentAccess(currentUser, departmentId);
        List<XpCurveDto> data = analyticsService.getXpCurve(yearNo, departmentId, stage, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/stage-distribution")
    public ResponseEntity<ApiResponse<List<StageDistributionDto>>> getStageDistribution(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<StageDistributionDto> data = analyticsService.getStageDistribution(yearNo, departmentId, stage, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceDto>>> getAttendanceTrend(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        List<AttendanceDto> data = analyticsService.getAttendanceTrend(yearNo, departmentId, stage, sectionId, startDate, endDate, period);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/attendance/calendar")
    public ResponseEntity<ApiResponse<List<AttendanceCalendarDto>>> getAttendanceCalendar(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        List<AttendanceCalendarDto> data = analyticsService.getAttendanceCalendar(yearNo, departmentId, stage, sectionId, startDate, endDate, period);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/activity-funnel")
    public ResponseEntity<ApiResponse<List<ActivityFunnelDto>>> getActivityFunnel(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<ActivityFunnelDto> data = analyticsService.getActivityFunnel(yearNo, departmentId, stage, sectionId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/performance-leaders")
    public ResponseEntity<ApiResponse<List<PerformanceLeaderDto>>> getTopPerformers(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        List<PerformanceLeaderDto> data = analyticsService.getTopPerformers(yearNo, departmentId, stage, startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/most-improved")
    public ResponseEntity<ApiResponse<List<MoverDto>>> getMostImproved(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        List<MoverDto> data = analyticsService.getMostImproved(yearNo, departmentId, stage, startDate, endDate, limit);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}