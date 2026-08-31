package jjcet.PragatiX.modules.analytics.api.dto.response.overview;

public record StageDistributionDto(
        String stageName,
        Long studentCount,
        Double percentage
) {
}