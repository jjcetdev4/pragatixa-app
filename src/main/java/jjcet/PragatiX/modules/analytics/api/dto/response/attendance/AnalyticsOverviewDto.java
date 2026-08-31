package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

public record AnalyticsOverviewDto(
        Double overallAttendancePercentage,
        Integer presentStudents,
        Integer partialAbsentees,
        Integer fullDayAbsentees,
        Integer totalStudents
) {
}