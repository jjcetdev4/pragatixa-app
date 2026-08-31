package jjcet.PragatiX.modules.analytics.api.dto.response.performance;

public record TopPerformerDto(
        Long studentId,
        String studentName,
        String registerNumber,
        Double xp,
        Double attendance
) {
}