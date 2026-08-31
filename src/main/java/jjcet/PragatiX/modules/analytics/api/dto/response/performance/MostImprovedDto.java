package jjcet.PragatiX.modules.analytics.api.dto.response.performance;

public record MostImprovedDto(
        Long studentId,
        String studentName,
        String rollNumber,
        Double improvementPercent
) {
}