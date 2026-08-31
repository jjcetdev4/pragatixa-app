package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record DepartmentXpAttendanceDto(
        String department,
        String month,
        Double avgXp,
        Double attendanceRate
) {
}