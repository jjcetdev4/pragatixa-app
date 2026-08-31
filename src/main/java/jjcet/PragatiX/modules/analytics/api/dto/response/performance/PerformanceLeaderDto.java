package jjcet.PragatiX.modules.analytics.api.dto.response.performance;

public record PerformanceLeaderDto(
        Long studentId,
        String studentName,
        String rollNumber,
        Double xp,
        Double attendance
) {
}