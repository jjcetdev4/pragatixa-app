package jjcet.PragatiX.modules.analytics.api.dto.response.attendance;

public record AttendanceSummaryRowDto(
        String departmentName,
        Integer presentOnly,
        Integer partial,
        Integer absentOnly,
        Double percentage,
        Integer totalStudents
) {
}