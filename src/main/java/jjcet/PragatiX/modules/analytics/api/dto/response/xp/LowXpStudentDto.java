package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record LowXpStudentDto(
        String studentName,
        String registerNumber,
        String department,
        String section,
        Long currentXp,
        Long xpGap
) {
}