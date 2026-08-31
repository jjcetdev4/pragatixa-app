package jjcet.PragatiX.modules.analytics.scoped.service;

import jjcet.PragatiX.entity.Department;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.service.AnalyticsService;
import jjcet.PragatiX.modules.analytics.service.attendance.AttendanceAnalyticsService;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HodScopedAnalyticsService {

    private final AnalyticsService analyticsService;
    private final AuthUtils authUtils;
    private final AttendanceAnalyticsService attendanceAnalyticsService;

    public HodScopedAnalyticsService(AnalyticsService analyticsService, AuthUtils authUtils, @Qualifier("legacyAttendanceAnalyticsService") AttendanceAnalyticsService attendanceAnalyticsService) {
        this.analyticsService = analyticsService;
        this.authUtils = authUtils;
        this.attendanceAnalyticsService = attendanceAnalyticsService;
    }

    public AllInOneDashboardDto getScopedDashboard(String yearNo, Integer stage, LocalDate startDate, LocalDate endDate) {
        User currentUser = authUtils.getCurrentUser();
        Department department = currentUser != null ? currentUser.getDepartment() : null;
        Long departmentId = department != null ? department.getId() : null;

        List<InstitutionGrowthDto> institutionGrowth = analyticsService.getInstitutionGrowth(yearNo, null, startDate, endDate);

        List<DepartmentGrowthDto> departmentGrowth = analyticsService.getDepartmentGrowth(yearNo, null, startDate, endDate);

        List<XpCurveDto> xpCurve = analyticsService.getXpCurve(yearNo, departmentId, stage, startDate, endDate);

        List<StageDistributionDto> stageDistribution = analyticsService.getStageDistribution(yearNo, departmentId, stage, startDate, endDate);

        List<AttendanceDto> attendance = analyticsService.getAttendanceTrend(yearNo, departmentId, stage, null, startDate, endDate, null);

        List<ActivityFunnelDto> activityFunnelList = analyticsService.getActivityFunnel(yearNo, departmentId, stage, null, startDate, endDate);
        ActivityFunnelWrapperDto activityFunnel = new ActivityFunnelWrapperDto(activityFunnelList);

        List<InterventionRiskDto> riskDtos = analyticsService.getInterventionRisks(yearNo, departmentId, stage, startDate, endDate);
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

        List<PerformanceLeaderDto> topPerformers = analyticsService.getTopPerformers(yearNo, departmentId, stage, startDate, endDate, 10);

        List<MoverDto> mostImproved = analyticsService.getMostImproved(yearNo, departmentId, stage, startDate, endDate, 10);

        long totalStudents = stageDistribution != null
                ? stageDistribution.stream().mapToLong(StageDistributionDto::studentCount).sum()
                : 0L;

        double sumOfXp = xpCurve != null
                ? xpCurve.stream().mapToDouble(xp -> xp.xp() != null ? xp.xp() : 0.0).sum()
                : 0.0;
        double totalXp;
        if (topPerformers != null && !topPerformers.isEmpty()) {
            totalXp = topPerformers.stream().mapToDouble(p -> p.xp() != null ? p.xp() : 0.0).sum();
        } else {
            totalXp = sumOfXp;
        }

        double avgXpAll = totalStudents > 0 ? totalXp / totalStudents : 0.0;

        double avgAttendance = 0.0;
        double avgEngagement = 0.0;
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

        if (institutionGrowth != null && !institutionGrowth.isEmpty()) {
            Double sumEngagement = 0.0;
            int engagementCount = 0;
            for (InstitutionGrowthDto dto : institutionGrowth) {
                if (dto.engagement() != null) {
                    sumEngagement += dto.engagement();
                    engagementCount++;
                }
            }
            avgEngagement = engagementCount > 0 ? sumEngagement / engagementCount : 0.0;
        }

        String topPerformer = (topPerformers != null && !topPerformers.isEmpty())
                ? topPerformers.get(0).studentName()
                : null;

        AllInOneDashboardDto dto = new AllInOneDashboardDto();
        dto.setTotalStudents(totalStudents);
        dto.setAvgXpAll(avgXpAll);
        dto.setTodayAttendance(null);
        dto.setTopPerformer(topPerformer);
        dto.setTopTeam(null);
        dto.setTotalXp(totalXp);
        dto.setAvgAttendance(avgAttendance);
        dto.setAvgEngagement(avgEngagement);
        dto.setInstitutionGrowth(institutionGrowth);
        dto.setDepartmentGrowth(departmentGrowth);
        dto.setXpCurve(xpCurve);
        dto.setStageDistribution(stageDistribution);
        dto.setAttendance(attendance);
        dto.setAttendanceSummary(attendanceAnalyticsService.getAttendanceSummaryByDate(yearNo, endDate != null ? endDate : LocalDate.now(), departmentId, stage, null, null));
        dto.setActivityFunnel(activityFunnel);
        dto.setInterventionRisk(interventionRisk);
        dto.setTopPerformers(topPerformers);
        dto.setMostImproved(mostImproved);

        return dto;
    }
}
