package jjcet.PragatiX.modules.analytics.api.dto.response.funnel;

public record ActivityFunnelDto(
        String stage,
        Long count,
        Double percentage
) {
}