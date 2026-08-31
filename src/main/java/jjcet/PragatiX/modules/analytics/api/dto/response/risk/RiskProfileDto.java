package jjcet.PragatiX.modules.analytics.api.dto.response.risk;

public record RiskProfileDto(
        Long studentId,
        String studentName,
        String department,
        Double attendancePercentage,
        Long daysSinceLastAttendance,
        Long totalXp,
        Long penaltyXp
) {
}