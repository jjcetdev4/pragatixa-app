package jjcet.PragatiX.modules.analytics.api.dto.response.funnel;

public record ActivityFunnelStageDto(
        String name,
        String description,
        Long count,
        Double percentage
) {
}