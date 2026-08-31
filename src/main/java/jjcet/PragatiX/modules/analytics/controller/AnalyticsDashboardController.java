package jjcet.PragatiX.modules.analytics.controller;

import jjcet.PragatiX.common.response.ApiResponse;
import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.service.AnalyticsService;
import jjcet.PragatiX.modules.analytics.service.attendance.AttendanceAnalyticsService;
import jjcet.PragatiX.modules.analytics.service.xp.XpAnalyticsService;
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
public class AnalyticsDashboardController {

    private final AttendanceAnalyticsService attendanceAnalyticsService;
    private final XpAnalyticsService xpAnalyticsService;
    private final AnalyticsService analyticsService;

    public AnalyticsDashboardController(AttendanceAnalyticsService attendanceAnalyticsService, XpAnalyticsService xpAnalyticsService, AnalyticsService analyticsService) {
        this.attendanceAnalyticsService = attendanceAnalyticsService;
        this.xpAnalyticsService = xpAnalyticsService;
        this.analyticsService = analyticsService;
    }


    @GetMapping("/attendance/summary")
    public ResponseEntity<ApiResponse<AttendanceSummaryDto>> getAttendanceSummary(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer period) {
        AttendanceSummaryDto data = attendanceAnalyticsService.getAttendanceSummaryByDate(academicYear, date, departmentId, stageId, sectionId, period);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/xp/distribution")
    public ResponseEntity<ApiResponse<List<XpDistributionDto>>> getXpDistribution(
            @RequestParam(required = false) String academicYear) {
        List<XpDistributionDto> data = xpAnalyticsService.getXpDistribution(academicYear);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/xp/department/monthly")
    public ResponseEntity<ApiResponse<List<DepartmentXpAttendanceDto>>> getMonthlyAvgXpByDepartment(
            @RequestParam(required = false) String academicYear) {
        List<DepartmentXpAttendanceDto> data = xpAnalyticsService.getMonthlyAvgXpByDepartment(academicYear);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/xp/department/alltime")
    public ResponseEntity<ApiResponse<List<DepartmentXpAttendanceDto>>> getAllTimeAvgXpByDepartment(
            @RequestParam(required = false) String academicYear) {
        List<DepartmentXpAttendanceDto> data = xpAnalyticsService.getAllTimeAvgXpByDepartment(academicYear);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/attendance/department/monthly")
    public ResponseEntity<ApiResponse<List<DepartmentXpAttendanceDto>>> getMonthlyAttendanceByDepartment(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Integer stageId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Integer period) {
        List<DepartmentXpAttendanceDto> data = attendanceAnalyticsService.getMonthlyAttendanceByDepartment(academicYear, departmentId, stageId, sectionId, period);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/leaderboard/top")
    public ResponseEntity<ApiResponse<List<TopPerformerDto>>> getTopPerformers(
            @RequestParam(defaultValue = "50") int limit) {
        List<TopPerformerDto> data = xpAnalyticsService.getTopPerformersNew(null, limit);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/teams/elite")
    public ResponseEntity<ApiResponse<List<EliteTeamDto>>> getTopEliteTeams(
            @RequestParam(defaultValue = "20") int limit) {
        List<EliteTeamDto> data = xpAnalyticsService.getTopEliteTeams(null, limit);
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<AllInOneDashboardDto>> getDashboardSummary(
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        AllInOneDashboardDto data = new AllInOneDashboardDto();

// Fetch all dashboard data through AnalyticsService
         data.setInstitutionGrowth(analyticsService.getInstitutionGrowth(academicYear, null, startDate, endDate));
         data.setDepartmentGrowth(analyticsService.getDepartmentGrowth(academicYear, null, startDate, endDate));
         data.setXpCurve(analyticsService.getXpCurve(academicYear, departmentId, null, startDate, endDate));
         data.setStageDistribution(analyticsService.getStageDistribution(academicYear, departmentId, null, startDate, endDate));
         data.setAttendance(analyticsService.getAttendanceTrend(academicYear, departmentId, null, null, startDate, endDate, null));
         data.setActivityFunnel(new ActivityFunnelWrapperDto(analyticsService.getActivityFunnel(academicYear, departmentId, null, null, startDate, endDate)));

         // Attendance summary
         data.setAttendanceSummary(attendanceAnalyticsService.getAttendanceSummaryByDate(academicYear, LocalDate.now(), departmentId, null, null, null));

// Intervention risk
         List<InterventionRiskDto> riskDtos = analyticsService.getInterventionRisks(academicYear, departmentId, null, startDate, endDate);
        Long totalStudentsRisk = riskDtos != null
                ? riskDtos.stream().mapToLong(InterventionRiskDto::studentCount).sum()
                : 0L;
        Long highCount = riskDtos != null
                ? riskDtos.stream().filter(r -> "HIGH".equals(r.riskLevel())).mapToLong(InterventionRiskDto::studentCount).sum()
                : 0L;
        Long mediumCount = riskDtos != null
                ? riskDtos.stream().filter(r -> "MEDIUM".equals(r.riskLevel())).mapToLong(InterventionRiskDto::studentCount).sum()
                : 0L;
        Long lowCount = riskDtos != null
                ? riskDtos.stream().filter(r -> "LOW".equals(r.riskLevel())).mapToLong(InterventionRiskDto::studentCount).sum()
                : 0L;

        InterventionStatusDto interventionRisk = new InterventionStatusDto(
                totalStudentsRisk, highCount, mediumCount, lowCount, riskDtos
        );
        data.setInterventionRisk(interventionRisk);

// Performance leaders
         List<PerformanceLeaderDto> topPerformers = analyticsService.getTopPerformers(academicYear, departmentId, null, startDate, endDate, 10);
         data.setTopPerformers(topPerformers);
         data.setMostImproved(analyticsService.getMostImproved(academicYear, departmentId, null, startDate, endDate, 10));

        // Summary metrics: totalStudents, totalXp, avgXpAll, avgAttendance, avgEngagement, topPerformer
        Long totalStudents = null;
        List<StageDistributionDto> stageDistribution = data.getStageDistribution();
        if (stageDistribution != null && !stageDistribution.isEmpty()) {
            totalStudents = stageDistribution.stream().mapToLong(StageDistributionDto::studentCount).sum();
        }
        if (totalStudents == null || totalStudents == 0L) {
            totalStudents = totalStudentsRisk;
        }
        data.setTotalStudents(totalStudents);

        double totalXp = 0.0;
        List<XpCurveDto> xpCurve = data.getXpCurve();
        if (xpCurve != null && !xpCurve.isEmpty()) {
            totalXp = xpCurve.stream().mapToDouble(xp -> xp.xp() != null ? xp.xp() : 0.0).sum();
        }
        data.setTotalXp(totalXp);

        Long totalStudentsForAvg = (totalStudents != null) ? totalStudents : 0L;
        double avgXpAll = (totalStudentsForAvg != null && totalStudentsForAvg > 0) ? totalXp / totalStudentsForAvg : 0.0;
        data.setAvgXpAll(avgXpAll);
        data.setAvgEngagement(avgXpAll);

        double avgAttendance = 0.0;
        List<AttendanceDto> attendance = data.getAttendance();
        if (attendance != null && !attendance.isEmpty()) {
            double sumRates = 0.0;
            int count = 0;
            for (AttendanceDto dto : attendance) {
                if (dto.rate() != null) {
                    sumRates += dto.rate();
                    count++;
                }
            }
            avgAttendance = count > 0 ? sumRates / count : 0.0;
        }
        data.setAvgAttendance(avgAttendance);

        String topPerformer = (topPerformers != null && !topPerformers.isEmpty())
                ? topPerformers.get(0).studentName()
                : null;
        data.setTopPerformer(topPerformer);
        data.setTopTeam(null);
        data.setTodayAttendance(null);

        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}