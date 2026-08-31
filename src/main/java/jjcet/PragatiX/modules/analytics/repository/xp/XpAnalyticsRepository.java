package jjcet.PragatiX.modules.analytics.repository.xp;

import jjcet.PragatiX.modules.analytics.dto.*;
import jjcet.PragatiX.modules.analytics.dto.AllInOneDashboardDto;
import jjcet.PragatiX.modules.analytics.dto.DepartmentXpAttendanceDto;
import jjcet.PragatiX.modules.analytics.dto.EliteTeamDto;
import jjcet.PragatiX.modules.analytics.dto.TopPerformerDto;
import jjcet.PragatiX.modules.analytics.dto.XpDistributionDto;
import java.time.LocalDate;
import java.util.List;

public interface XpAnalyticsRepository {

    List<XpAwardVsPenaltyDTO> getAwardVsPenalty(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate);
    List<GroupedXpDTO> getDepartmentRanking(String yearNo, Integer stage, LocalDate startDate, LocalDate endDate);
    List<GroupedXpDTO> getSectionRanking(String yearNo, Long departmentId, Integer stage, LocalDate startDate, LocalDate endDate);
    List<XpHeatmapDTO> getMonthlyHeatmap(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate);
    List<XpTopPerformerDTO> getTopPerformers(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate);
    List<LowXpStudentDTO> getLowXpStudents(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, Long threshold);
    List<ActivityXpContributionDTO> getActivityXpContribution(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String category);
    List<XpHistoryDTO> getXpHistory(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type, int limit, int offset);
    long getXpHistoryCount(String yearNo, Long departmentId, Integer stage, Long sectionId, LocalDate startDate, LocalDate endDate, String activityName, String type);
    
    List<XpDistributionDto> getXpDistribution(String yearNo);
    List<DepartmentXpAttendanceDto> getMonthlyAvgXpByDepartment(String yearNo);
    List<DepartmentXpAttendanceDto> getAllTimeAvgXpByDepartment(String yearNo);
    List<TopPerformerDto> getTopPerformersNew(String yearNo, int limit);
    List<EliteTeamDto> getTopEliteTeams(String yearNo, int limit);
    AllInOneDashboardDto getDashboardSummary(String yearNo);
}
