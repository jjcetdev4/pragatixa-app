package jjcet.PragatiX.modules.analytics.api.dto.response.overview;

import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.attendance.AttendanceSummaryDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.funnel.ActivityFunnelWrapperDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.MostImprovedDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.performance.PerformanceLeaderDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.risk.InterventionStatusDto;
import jjcet.PragatiX.modules.analytics.api.dto.response.xp.XpCurveDto;
import java.util.List;

public record AllInOneDashboardDto(
        Long totalStudents,
        Double avgXpAll,
        Double todayAttendance,
        String topPerformer,
        String topTeam,
        Double totalXp,
        Double avgAttendance,
        Double avgEngagement,
        List<InstitutionGrowthDto> institutionGrowth,
        List<DepartmentGrowthDto> departmentGrowth,
        List<XpCurveDto> xpCurve,
        List<StageDistributionDto> stageDistribution,
        List<AttendanceDto> attendance,
        AttendanceSummaryDto attendanceSummary,
        ActivityFunnelWrapperDto activityFunnel,
        InterventionStatusDto interventionRisk,
        List<PerformanceLeaderDto> topPerformers,
        List<MostImprovedDto> mostImproved
) {
    public static AllInOneDashboardDto empty() {
        return new AllInOneDashboardDto(
                0L, 0.0, null, null, null,
                0.0, 0.0, 0.0,
                List.of(), List.of(), List.of(), List.of(), List.of(),
                AttendanceSummaryDto.empty(), ActivityFunnelWrapperDto.empty(),
                InterventionStatusDto.empty(), List.of(), List.of()
        );
    }
}