package jjcet.PragatiX.modules.analytics.api.dto.response.overview;

public record InstitutionGrowthDto(
        String period,
        Double overallGrowth,
        Double xpGrowth,
        Double attendance,
        Double engagement,
        Integer stage
) {
}