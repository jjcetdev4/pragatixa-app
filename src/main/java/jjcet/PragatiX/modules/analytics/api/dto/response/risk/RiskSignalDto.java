package jjcet.PragatiX.modules.analytics.api.dto.response.risk;

public record RiskSignalDto(
        String riskLevel,
        Long studentCount
) {
}