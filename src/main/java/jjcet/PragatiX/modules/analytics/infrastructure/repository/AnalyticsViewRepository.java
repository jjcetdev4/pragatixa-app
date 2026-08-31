package jjcet.PragatiX.modules.analytics.infrastructure.repository;

import jjcet.PragatiX.modules.analytics.api.dto.request.AnalyticsFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.AttendanceFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.FunnelFilter;
import jjcet.PragatiX.modules.analytics.api.dto.request.XpFilter;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AnalyticsOverviewDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceCalendarDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceExportDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceSummaryDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceSummaryRowDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceTrendDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.GroupedAttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.LowAttendanceStudentDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.funnel.ActivityFunnelDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.DepartmentGrowthDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.InstitutionGrowthDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.overview.StageDistributionDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.MostImprovedDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.PerformanceLeaderDto;
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
import java.util.List;

public interface AnalyticsViewRepository {

    List<InstitutionGrowthDto> findInstitutionGrowth(AnalyticsFilter filter);

    List<DepartmentGrowthDto> findDepartmentGrowth(AnalyticsFilter filter);

    List<XpCurveDto> findXpCurve(AnalyticsFilter filter);

    List<StageDistributionDto> findStageDistribution(AnalyticsFilter filter);

    List<AttendanceDto> findAttendanceTrend(AttendanceFilter filter);

    List<AttendanceCalendarDto> findAttendanceCalendar(AttendanceFilter filter);

    List<ActivityFunnelDto> findActivityFunnel(FunnelFilter filter);

    List<RiskProfileDto> findRiskProfiles(AnalyticsFilter filter);

    List<RiskSignalDto> aggregateRiskSignals(List<RiskProfileDto> profiles);

    List<PerformanceLeaderDto> findTopPerformers(AnalyticsFilter filter, int limit);

    List<MostImprovedDto> findMostImproved(AnalyticsFilter filter, int limit);

    AnalyticsOverviewDto findAttendanceOverview(AttendanceFilter filter);

    List<AttendanceTrendDto> findAttendanceTrends(AttendanceFilter filter);

    AttendanceDistributionDto findAttendanceDistribution(AttendanceFilter filter);

    List<GroupedAttendanceDto> findDepartmentWiseAttendance(AttendanceFilter filter);

    List<LowAttendanceStudentDto> findLowAttendanceStudents(AttendanceFilter filter);

    List<GroupedAttendanceDto> findSectionWiseAttendance(AttendanceFilter filter);

    List<AttendanceSummaryRowDto> findAttendanceSummaryTable(AttendanceFilter filter);

    List<AttendanceExportDto> findAttendanceExportData(AttendanceFilter filter);

    AttendanceSummaryDto findAttendanceSummaryByDate(AttendanceFilter filter);

    List<DepartmentXpAttendanceDto> findMonthlyAttendanceByDepartment(AttendanceFilter filter);

    List<XpAwardVsPenaltyDto> findAwardVsPenalty(XpFilter filter);

    List<GroupedXpDto> findDepartmentRanking(XpFilter filter);

    List<GroupedXpDto> findSectionRanking(XpFilter filter);

    List<XpHeatmapDto> findMonthlyHeatmap(XpFilter filter);

    List<XpTopPerformerDto> findXpTopPerformers(XpFilter filter);

    List<LowXpStudentDto> findLowXpStudents(XpFilter filter);

    List<ActivityXpContributionDto> findActivityXpContribution(XpFilter filter);

    List<XpHistoryDto> findXpHistory(XpFilter filter);

    long countXpHistory(XpFilter filter);

    List<XpDistributionDto> findXpDistribution(String yearNo);

    List<DepartmentXpAttendanceDto> findMonthlyAvgXpByDepartment(String yearNo);

    List<DepartmentXpAttendanceDto> findAllTimeAvgXpByDepartment(String yearNo);

    List<TopPerformerDto> findTopPerformersNew(String yearNo, int limit);

    List<EliteTeamDto> findEliteTeams(String yearNo, int limit);
}