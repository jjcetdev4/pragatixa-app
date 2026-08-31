package jjcet.PragatiX.modules.analytics.repository;

import jjcet.PragatiX.modules.analytics.dto.*;
import java.time.LocalDate;
import java.util.List;

public interface AnalyticsRepository {

    // Institution Growth
    List<InstitutionGrowthDto> getInstitutionGrowth(String yearNo, Integer semester, LocalDate startDate, LocalDate endDate);

    // Department Growth
    List<DepartmentGrowthDto> getDepartmentGrowth(String yearNo, Integer semester, LocalDate startDate, LocalDate endDate);

    // XP Curve
    List<XpCurveDto> getXpCurve(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate);

    // Stage Distribution
    List<StageDistributionDto> getStageDistribution(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate);

    // Attendance
    List<AttendanceDto> getAttendanceTrend(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period);
    List<AttendanceCalendarDto> getAttendanceCalendar(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Integer period);

    // Activity Funnel
    List<ActivityFunnelDto> getActivityFunnel(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate);

    // Intervention Risk
    List<Object[]> getInterventionRisks(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate);

    // Performance Leaders
    List<PerformanceLeaderDto> getTopPerformers(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer limit);
    List<MoverDto> getMostImproved(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate, Integer limit);
}