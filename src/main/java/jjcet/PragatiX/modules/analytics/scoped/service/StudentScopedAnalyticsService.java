package jjcet.PragatiX.modules.analytics.scoped.service;

import jjcet.PragatiX.entity.Student;
import jjcet.PragatiX.entity.User;
import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.service.AnalyticsService;
import jjcet.PragatiX.modules.analytics.service.attendance.AttendanceAnalyticsService;
import jjcet.PragatiX.modules.authentication.security.AuthUtils;
import jjcet.PragatiX.modules.student.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Service
public class StudentScopedAnalyticsService {

    private final AnalyticsService analyticsService;
    private final AuthUtils authUtils;
    private final StudentRepository studentRepository;
    private final AttendanceAnalyticsService attendanceAnalyticsService;

    public StudentScopedAnalyticsService(AnalyticsService analyticsService,
                                          AuthUtils authUtils,
                                          StudentRepository studentRepository,
                                          @Qualifier("legacyAttendanceAnalyticsService") AttendanceAnalyticsService attendanceAnalyticsService) {
        this.analyticsService = analyticsService;
        this.authUtils = authUtils;
        this.studentRepository = studentRepository;
        this.attendanceAnalyticsService = attendanceAnalyticsService;
    }

    public AllInOneDashboardDto getScopedDashboard(String yearNo, Integer stage, LocalDate startDate, LocalDate endDate) {
        User currentUser = authUtils.getCurrentUser();
        if (currentUser == null) {
            return createEmptyDashboard();
        }

        Student student = studentRepository.findByUserId(currentUser.getId()).orElse(null);
        if (student == null) {
            return createEmptyDashboard();
        }

        Long departmentId = student.getDepartment() != null ? student.getDepartment().getId() : null;
        Long sectionId = student.getSection() != null ? student.getSection().getId() : null;
        int studentStage = student.getStage();
        Integer effectiveStage = stage != null ? stage : (studentStage > 0 ? studentStage : null);

        List<InstitutionGrowthDto> institutionGrowth = analyticsService.getInstitutionGrowth(yearNo, null, startDate, endDate);
        List<DepartmentGrowthDto> departmentGrowth = analyticsService.getDepartmentGrowth(yearNo, null, startDate, endDate);
        List<XpCurveDto> xpCurve = analyticsService.getXpCurve(yearNo, departmentId, effectiveStage, startDate, endDate);
        List<StageDistributionDto> stageDistribution = analyticsService.getStageDistribution(yearNo, departmentId, effectiveStage, startDate, endDate);
        List<AttendanceDto> attendance = analyticsService.getAttendanceTrend(yearNo, departmentId, effectiveStage, sectionId, startDate, endDate, null);
        List<ActivityFunnelDto> activityFunnelList = analyticsService.getActivityFunnel(yearNo, departmentId, effectiveStage, sectionId, startDate, endDate);
        ActivityFunnelWrapperDto activityFunnel = new ActivityFunnelWrapperDto(activityFunnelList);

        List<InterventionRiskDto> riskDtos = safeRisk(analyticsService.getInterventionRisks(yearNo, departmentId, effectiveStage, startDate, endDate));
        InterventionStatusDto interventionRisk = new InterventionStatusDto(
                riskDtos.stream().mapToLong(InterventionRiskDto::studentCount).sum(),
                riskDtos.stream().filter(r -> "HIGH".equals(r.riskLevel())).mapToLong(InterventionRiskDto::studentCount).sum(),
                riskDtos.stream().filter(r -> "MEDIUM".equals(r.riskLevel())).mapToLong(InterventionRiskDto::studentCount).sum(),
                riskDtos.stream().filter(r -> "LOW".equals(r.riskLevel())).mapToLong(InterventionRiskDto::studentCount).sum(),
                riskDtos
        );

        List<PerformanceLeaderDto> topPerformers = analyticsService.getTopPerformers(yearNo, departmentId, effectiveStage, startDate, endDate, 10);
        List<MoverDto> mostImproved = analyticsService.getMostImproved(yearNo, departmentId, effectiveStage, startDate, endDate, 10);

        long totalStudents = stageDistribution != null
                ? stageDistribution.stream().mapToLong(StageDistributionDto::studentCount).sum()
                : 0L;

        double sumOfXp = xpCurve != null
                ? xpCurve.stream().mapToDouble(xp -> xp.xp() != null ? xp.xp() : 0.0).sum()
                : 0.0;
        double totalXp = topPerformers != null && !topPerformers.isEmpty()
                ? topPerformers.stream().mapToDouble(p -> p.xp() != null ? p.xp() : 0.0).sum()
                : sumOfXp;
        double avgXpAll = totalStudents > 0 ? totalXp / totalStudents : 0.0;

        double avgAttendance = 0.0;
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

        double avgEngagement = 0.0;
        if (institutionGrowth != null && !institutionGrowth.isEmpty()) {
            double sumEngagement = 0.0;
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
        dto.setAttendanceSummary(attendanceAnalyticsService.getAttendanceSummaryByDate(
                yearNo, endDate != null ? endDate : LocalDate.now(), departmentId, effectiveStage, sectionId, null));
        dto.setActivityFunnel(activityFunnel);
        dto.setInterventionRisk(interventionRisk);
        dto.setTopPerformers(topPerformers);
        dto.setMostImproved(mostImproved);

        return dto;
    }

    private List<InterventionRiskDto> safeRisk(List<InterventionRiskDto> in) {
        return in != null ? in : Collections.emptyList();
    }

    private AllInOneDashboardDto createEmptyDashboard() {
        AllInOneDashboardDto dto = new AllInOneDashboardDto();
        dto.setTotalStudents(0L);
        dto.setAvgXpAll(0.0);
        dto.setTotalXp(0.0);
        dto.setAvgAttendance(0.0);
        dto.setAvgEngagement(0.0);
        dto.setInstitutionGrowth(new java.util.ArrayList<>());
        dto.setDepartmentGrowth(new java.util.ArrayList<>());
        dto.setXpCurve(new java.util.ArrayList<>());
        dto.setStageDistribution(new java.util.ArrayList<>());
        dto.setAttendance(new java.util.ArrayList<>());
        dto.setTopPerformers(new java.util.ArrayList<>());
        dto.setMostImproved(new java.util.ArrayList<>());
        return dto;
    }
}
