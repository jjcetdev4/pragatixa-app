package jjcet.PragatiX.modules.analytics.api.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.analytics.api.dto.request.AnalyticsFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.AttendanceFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.FunnelFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.XpFilter;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AnalyticsOverviewDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceCalendarDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceSummaryDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceSummaryRowDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceTrendDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.GroupedAttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.LowAttendanceStudentDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.funnel.ActivityFunnelDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.funnel.ActivityFunnelWrapperDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.AllInOneDashboardDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.DepartmentGrowthDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.InstitutionGrowthDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.StageDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.MostImprovedDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.PerformanceLeaderDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.InterventionStatusDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.RiskProfileDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.RiskSignalDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.ActivityXpContributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.DepartmentXpAttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.EliteTeamDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.GroupedXpDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.LowXpStudentDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.TopPerformerDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpAwardVsPenaltyDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpCurveDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpHeatmapDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpHistoryDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpTopPerformerDto;
import jjcet.PragatiX.modules.analytics.domain.scope.ScopeAware;
import jjcet.PragatiX.modules.analytics.domain.service.AnalyticsOrchestrator;
import jjcet.PragatiX.modules.analytics.domain.service.attendance.AttendanceAnalyticsService;
import jjcet.PragatiX.modules.analytics.domain.service.funnel.ActivityFunnelService;
import jjcet.PragatiX.modules.analytics.domain.service.performance.PerformanceService;
import jjcet.PragatiX.modules.analytics.domain.service.xp.XpAnalyticsService;
import jjcet.PragatiX.modules.analytics.infrastructure.repository.AnalyticsViewRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN', 'ROLE_SUPER_ADMIN', 'SUPER_ADMIN', 'ROLE_HOD', 'HOD', 'ROLE_FACULTY', 'FACULTY', 'ROLE_TEACHER', 'TEACHER', 'ROLE_STUDENT', 'STUDENT', 'ROLE_CAPTAIN', 'CAPTAIN')")
@ScopeAware
public class AnalyticsController {

    private final AnalyticsViewRepository repository;
    private final AttendanceAnalyticsService attendanceService;
    private final XpAnalyticsService xpService;
    private final ActivityFunnelService funnelService;
    private final PerformanceService performanceService;
    private final AnalyticsOrchestrator orchestrator;

    public AnalyticsController(AnalyticsViewRepository repository,
                               AttendanceAnalyticsService attendanceService,
                               XpAnalyticsService xpService,
                               ActivityFunnelService funnelService,
                               PerformanceService performanceService,
                               AnalyticsOrchestrator orchestrator) {
        this.repository = repository;
        this.attendanceService = attendanceService;
        this.xpService = xpService;
        this.funnelService = funnelService;
        this.performanceService = performanceService;
        this.orchestrator = orchestrator;
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<AllInOneDashboardDto>> getOverview(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AllInOneDashboardDto data = orchestrator.getDashboardSummary(academicYear, departmentId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/institution-growth")
    public ResponseEntity<ApiResponse<List<InstitutionGrowthDto>>> getInstitutionGrowth(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(repository.findInstitutionGrowth(
                AnalyticsFilter.builder().yearNo(yearNo).semester(semester).startDate(startDate).endDate(endDate).build())));
    }

    @GetMapping("/department-growth")
    public ResponseEntity<ApiResponse<List<DepartmentGrowthDto>>> getDepartmentGrowth(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(repository.findDepartmentGrowth(
                AnalyticsFilter.builder().yearNo(yearNo).semester(semester).startDate(startDate).endDate(endDate).build())));
    }

    @GetMapping("/xp-curve")
    public ResponseEntity<ApiResponse<List<XpCurveDto>>> getXpCurve(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(repository.findXpCurve(
                AnalyticsFilter.builder().yearNo(yearNo).departmentId(departmentId).stage(stage).startDate(startDate).endDate(endDate).build())));
    }

    @GetMapping("/stage-distribution")
    public ResponseEntity<ApiResponse<List<StageDistributionDto>>> getStageDistribution(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(repository.findStageDistribution(
                AnalyticsFilter.builder().yearNo(yearNo).departmentId(departmentId).stage(stage).startDate(startDate).endDate(endDate).build())));
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
        return ResponseEntity.ok(ApiResponse.ok(repository.findAttendanceTrend(
                AttendanceFilter.builder().academicYear(yearNo).departmentId(departmentId).stageId(stage).sectionId(sectionId).startDate(startDate).endDate(endDate).period(period).build())));
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
        return ResponseEntity.ok(ApiResponse.ok(repository.findAttendanceCalendar(
                AttendanceFilter.builder().academicYear(yearNo).departmentId(departmentId).stageId(stage).sectionId(sectionId).startDate(startDate).endDate(endDate).period(period).build())));
    }

    @GetMapping("/activity-funnel")
    public ResponseEntity<ApiResponse<List<ActivityFunnelDto>>> getActivityFunnel(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(ApiResponse.ok(repository.findActivityFunnel(
                FunnelFilter.builder().academicYear(yearNo).departmentId(departmentId).stageId(stage).sectionId(sectionId).startDate(startDate).endDate(endDate).build())));
    }

    @GetMapping("/performance-leaders")
    public ResponseEntity<ApiResponse<List<PerformanceLeaderDto>>> getTopPerformers(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(performanceService.getTopPerformers(
                AnalyticsFilter.builder().yearNo(yearNo).departmentId(departmentId).stage(stage).startDate(startDate).endDate(endDate).build(),
                limit != null ? limit : 10)));
    }

    @GetMapping("/most-improved")
    public ResponseEntity<ApiResponse<List<MostImprovedDto>>> getMostImproved(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(ApiResponse.ok(performanceService.getMostImproved(
                AnalyticsFilter.builder().yearNo(yearNo).departmentId(departmentId).stage(stage).startDate(startDate).endDate(endDate).build(),
                limit != null ? limit : 10)));
    }

    @GetMapping("/intervention-risks")
    public ResponseEntity<ApiResponse<InterventionStatusDto>> getInterventionRisks(
            @RequestParam(required = false) String yearNo,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stage,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        LocalDate effectiveStart = startDate != null ? startDate : LocalDate.of(2000, 1, 1);
        LocalDate effectiveEnd = endDate != null ? endDate : LocalDate.of(2030, 12, 31);
        List<RiskProfileDto> profiles = repository.findRiskProfiles(
                AnalyticsFilter.builder().yearNo(yearNo).departmentId(departmentId).stage(stage).startDate(effectiveStart).endDate(effectiveEnd).build());
        List<RiskSignalDto> signals = repository.aggregateRiskSignals(profiles);
        long total = profiles.size();
        long high = signals.stream().filter(r -> "HIGH".equals(r.riskLevel())).mapToLong(RiskSignalDto::studentCount).sum();
        long medium = signals.stream().filter(r -> "MEDIUM".equals(r.riskLevel())).mapToLong(RiskSignalDto::studentCount).sum();
        long low = signals.stream().filter(r -> "LOW".equals(r.riskLevel())).mapToLong(RiskSignalDto::studentCount).sum();
        return ResponseEntity.ok(ApiResponse.ok(new InterventionStatusDto(total, high, medium, low, signals)));
    }

    @GetMapping("/attendance/overview")
    public ResponseEntity<AnalyticsOverviewDto> getAttendanceOverview(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(attendanceService.getOverview(
                AttendanceFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).period(period).build()));
    }

    @GetMapping("/attendance/trend")
    public ResponseEntity<List<AttendanceTrendDto>> getAttendanceTrendDetailed(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(attendanceService.getTrend(
                AttendanceFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).period(period).build()));
    }

    @GetMapping("/attendance/distribution")
    public ResponseEntity<AttendanceDistributionDto> getAttendanceDistribution(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(attendanceService.getDistribution(
                AttendanceFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).period(period).build()));
    }

    @GetMapping("/attendance/departments")
    public ResponseEntity<List<GroupedAttendanceDto>> getDepartmentWiseAttendance(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(attendanceService.getDepartmentWiseAttendance(
                AttendanceFilter.builder().academicYear(academicYear).startDate(startDate).endDate(endDate).period(period).build()));
    }

    @GetMapping("/attendance/low-attendance")
    public ResponseEntity<List<LowAttendanceStudentDto>> getLowAttendanceStudents(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period,
            @RequestParam(required = false, defaultValue = "75.0") Double threshold) {
        return ResponseEntity.ok(attendanceService.getLowAttendanceStudents(
                AttendanceFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).period(period).threshold(threshold).build()));
    }

    @GetMapping("/attendance/sections")
    public ResponseEntity<List<GroupedAttendanceDto>> getSectionWiseAttendance(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(attendanceService.getSectionWiseAttendance(
                AttendanceFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).startDate(startDate).endDate(endDate).period(period).build()));
    }

    @GetMapping("/attendance/summary-table")
    public ResponseEntity<List<AttendanceSummaryRowDto>> getAttendanceSummaryTable(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(attendanceService.getSummaryTable(
                AttendanceFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).period(period).build()));
    }

    @GetMapping("/attendance/summary")
    public ResponseEntity<AttendanceSummaryDto> getAttendanceSummary(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(attendanceService.getAttendanceSummaryByDate(
                AttendanceFilter.builder().academicYear(academicYear).date(date).departmentId(departmentId).stageId(stageId).sectionId(sectionId).period(period).build()));
    }

    @GetMapping("/attendance/export")
    public ResponseEntity<byte[]> exportAttendanceReport(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Integer period) {

        byte[] excelData = attendanceService.exportAttendanceReport(
                AttendanceFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).period(period).build());

        String dateStr = (startDate != null) ? startDate.toString() : LocalDate.now().toString();
        String prefix = (departmentId != null) ? (sectionId != null ? "SectionReport_" : "DepartmentReport_") : "AttendanceReport_";
        String filename = prefix + dateStr + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
    }

    @GetMapping("/attendance/department/monthly")
    public ResponseEntity<List<DepartmentXpAttendanceDto>> getMonthlyAttendanceByDepartment(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer period) {
        return ResponseEntity.ok(attendanceService.getMonthlyAttendanceByDepartment(
                AttendanceFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).period(period).build()));
    }

    @GetMapping("/xp/award-penalty")
    public ResponseEntity<List<XpAwardVsPenaltyDto>> getAwardVsPenalty(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(xpService.getAwardVsPenalty(
                XpFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).build()));
    }

    @GetMapping("/xp/distribution")
    public ResponseEntity<List<XpDistributionDto>> getXpDistribution(
            @RequestParam(required = false) String academicYear) {
        return ResponseEntity.ok(xpService.getXpDistribution(academicYear));
    }

    @GetMapping("/xp/department/monthly")
    public ResponseEntity<List<DepartmentXpAttendanceDto>> getMonthlyAvgXpByDepartment(
            @RequestParam(required = false) String yearNo) {
        return ResponseEntity.ok(xpService.getMonthlyAvgXpByDepartment(yearNo));
    }

    @GetMapping("/xp/department/alltime")
    public ResponseEntity<List<DepartmentXpAttendanceDto>> getAllTimeAvgXpByDepartment(
            @RequestParam(required = false) String yearNo) {
        return ResponseEntity.ok(xpService.getAllTimeAvgXpByDepartment(yearNo));
    }

    @GetMapping("/leaderboard/top")
    public ResponseEntity<List<TopPerformerDto>> getTopPerformersLeaderboard(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(xpService.getTopPerformersNew(null, limit));
    }

    @GetMapping("/teams/elite")
    public ResponseEntity<List<EliteTeamDto>> getTopEliteTeams(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(xpService.getTopEliteTeams(null, limit));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<AllInOneDashboardDto>> getDashboardSummary(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AllInOneDashboardDto data = orchestrator.getDashboardSummary(academicYear, departmentId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/xp/departments")
    public ResponseEntity<List<GroupedXpDto>> getDepartmentRanking(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(xpService.getDepartmentRanking(
                XpFilter.builder().academicYear(academicYear).stageId(stageId).startDate(startDate).endDate(endDate).build()));
    }

    @GetMapping("/xp/sections")
    public ResponseEntity<List<GroupedXpDto>> getSectionRanking(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(xpService.getSectionRanking(
                XpFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).startDate(startDate).endDate(endDate).build()));
    }

    @GetMapping("/xp/heatmap")
    public ResponseEntity<List<XpHeatmapDto>> getMonthlyHeatmap(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(xpService.getMonthlyHeatmap(
                XpFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).build()));
    }

    @GetMapping("/xp/top-performers")
    public ResponseEntity<List<XpTopPerformerDto>> getXpTopPerformers(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(xpService.getTopPerformers(
                XpFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).build()));
    }

    @GetMapping("/xp/low-xp")
    public ResponseEntity<List<LowXpStudentDto>> getLowXpStudents(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "20") Long threshold) {
        return ResponseEntity.ok(xpService.getLowXpStudents(
                XpFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).threshold(threshold).build()));
    }

    @GetMapping("/xp/activities")
    public ResponseEntity<List<ActivityXpContributionDto>> getActivityXpContribution(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(xpService.getActivityXpContribution(
                XpFilter.builder().academicYear(academicYear).departmentId(departmentId).stageId(stageId).sectionId(sectionId).startDate(startDate).endDate(endDate).category(category).build()));
    }

    @GetMapping("/xp/history")
    public ResponseEntity<Map<String, Object>> getXpHistory(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        XpFilter filter = XpFilter.builder()
                .academicYear(academicYear)
                .departmentId(departmentId)
                .stageId(stageId)
                .sectionId(sectionId)
                .startDate(startDate)
                .endDate(endDate)
                .category(activityName)
                .type(type)
                .page(page)
                .size(size)
                .build();

        List<XpHistoryDto> data = xpService.getXpHistory(filter);
        long total = xpService.getXpHistoryCount(filter);

        return ResponseEntity.ok(Map.of(
                "content", data,
                "totalElements", total,
                "totalPages", (int) Math.ceil((double) total / size),
                "currentPage", page
        ));
    }

    @GetMapping("/xp/export-history")
    public ResponseEntity<byte[]> exportXpHistory(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String activityName,
            @RequestParam(required = false) String type) {

        XpFilter filter = XpFilter.builder()
                .academicYear(academicYear)
                .departmentId(departmentId)
                .stageId(stageId)
                .sectionId(sectionId)
                .startDate(startDate)
                .endDate(endDate)
                .category(activityName)
                .type(type)
                .size(Integer.MAX_VALUE)
                .page(0)
                .build();

        byte[] data = xpService.exportXpHistory(filter);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"XP_History_Report.xlsx\"");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok", "module", "analytics"));
    }
}