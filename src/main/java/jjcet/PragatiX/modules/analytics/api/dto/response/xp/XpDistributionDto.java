package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record XpDistributionDto(
        String xpRange,
        Long studentCount,
        Double percentage
) {
}