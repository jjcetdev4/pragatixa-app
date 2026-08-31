package jjcet.PragatiX.modules.analytics.domain.service;

import jjcet.PragatiX.modules.analytics.api.dto.request.AnalyticsFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.AttendanceFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.FunnelFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.XpFilter;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceSummaryDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.AllInOneDashboardDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.DepartmentGrowthDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.InstitutionGrowthDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.StageDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.funnel.ActivityFunnelWrapperDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.MostImprovedDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.PerformanceLeaderDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.InterventionStatusDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.RiskProfileDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.RiskSignalDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpCurveDto;
import jjcet.PragatiX.modules.analytics.domain.model.Scope;
import jjcet.PragatiX.modules.analytics.domain.scope.ScopeResolver;
import jjcet.PragatiX.modules.analytics.domain.service.attendance.AttendanceAnalyticsService;
import jjcet.PragatiX.modules.analytics.domain.service.funnel.ActivityFunnelService;
import jjcet.PragatiX.modules.analytics.domain.service.performance.PerformanceService;
import jjcet.PragatiX.modules.analytics.infrastructure.repository.AnalyticsViewRepository;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsOrchestrator {

    private final AnalyticsViewRepository repository;
    private final AttendanceAnalyticsService attendanceService;
    private final ActivityFunnelService funnelService;
    private final PerformanceService performanceService;
    private final ScopeResolver scopeResolver;
    private final AuthUtils authUtils;

    public AnalyticsOrchestrator(AnalyticsViewRepository repository,
                                 AttendanceAnalyticsService attendanceService,
                                 ActivityFunnelService funnelService,
                                 PerformanceService performanceService,
                                 ScopeResolver scopeResolver,
                                 AuthUtils authUtils) {
        this.repository = repository;
        this.attendanceService = attendanceService;
        this.funnelService = funnelService;
        this.performanceService = performanceService;
        this.scopeResolver = scopeResolver;
        this.authUtils = authUtils;
    }

    public AllInOneDashboardDto getDashboardSummary(String academicYear,
                                                    Long departmentId,
                                                    LocalDate startDate,
                                                    LocalDate endDate) {
        User user = authUtils.getCurrentUser();
        Scope scope = scopeResolver.resolveScope(user);
        Long effectiveDept = scope.level() == Scope.ScopeLevel.INSTITUTION ? departmentId : scope.departmentId();

        AnalyticsFilter baseFilter = AnalyticsFilter.builder()
                .yearNo(academicYear)
                .departmentId(effectiveDept)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        AttendanceFilter attendanceFilter = AttendanceFilter.builder()
                .academicYear(academicYear)
                .departmentId(effectiveDept)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        FunnelFilter funnelFilter = FunnelFilter.builder()
                .academicYear(academicYear)
                .departmentId(effectiveDept)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        List<InstitutionGrowthDto> institutionGrowth = repository.findInstitutionGrowth(baseFilter);
        List<DepartmentGrowthDto> departmentGrowth = repository.findDepartmentGrowth(baseFilter);
        List<XpCurveDto> xpCurve = repository.findXpCurve(baseFilter);
        List<StageDistributionDto> stageDistribution = repository.findStageDistribution(baseFilter);
        List<AttendanceDto> attendance = repository.findAttendanceTrend(attendanceFilter);
        ActivityFunnelWrapperDto activityFunnel = funnelService.getFunnel(funnelFilter);
        AttendanceSummaryDto attendanceSummary = attendanceService.getAttendanceSummaryByDate(
                AttendanceFilter.builder()
                        .academicYear(academicYear)
                        .departmentId(effectiveDept)
                        .date(endDate != null ? endDate : LocalDate.now())
                        .build());

        List<RiskProfileDto> riskProfiles = repository.findRiskProfiles(baseFilter);
        List<RiskSignalDto> riskSignals = repository.aggregateRiskSignals(riskProfiles);
        long totalAtRisk = riskProfiles.size();
        long high = riskSignals.stream().filter(r -> "HIGH".equals(r.riskLevel())).mapToLong(RiskSignalDto::studentCount).sum();
        long medium = riskSignals.stream().filter(r -> "MEDIUM".equals(r.riskLevel())).mapToLong(RiskSignalDto::studentCount).sum();
        long low = riskSignals.stream().filter(r -> "LOW".equals(r.riskLevel())).mapToLong(RiskSignalDto::studentCount).sum();
        InterventionStatusDto interventionRisk = new InterventionStatusDto(totalAtRisk, high, medium, low, riskSignals);

        List<PerformanceLeaderDto> topPerformers = performanceService.getTopPerformers(baseFilter, 10);
        List<MostImprovedDto> mostImproved = performanceService.getMostImproved(baseFilter, 10);

        long totalStudents = stageDistribution != null
                ? stageDistribution.stream().mapToLong(StageDistributionDto::studentCount).sum()
                : 0L;

        double totalXp = xpCurve != null
                ? xpCurve.stream().mapToDouble(x -> x.xp() != null ? x.xp() : 0.0).sum()
                : 0.0;

        double avgXp = totalStudents > 0 ? totalXp / totalStudents : 0.0;

        double avgAttendance = 0.0;
        if (attendance != null && !attendance.isEmpty()) {
            double sum = 0;
            int count = 0;
            for (AttendanceDto dto : attendance) {
                if (dto.rate() != null) {
                    sum += dto.rate();
                    count++;
                }
            }
            avgAttendance = count > 0 ? sum / count : 0.0;
        }

        String topPerformer = (topPerformers != null && !topPerformers.isEmpty())
                ? topPerformers.get(0).studentName() : null;

        return new AllInOneDashboardDto(
                totalStudents,
                avgXp,
                null,
                topPerformer,
                null,
                totalXp,
                avgAttendance,
                avgXp,
                institutionGrowth,
                departmentGrowth,
                xpCurve,
                stageDistribution,
                attendance,
                attendanceSummary,
                activityFunnel,
                interventionRisk,
                topPerformers,
                mostImproved
        );
    }
}