package jjcet.PragatiX.modules.analytics.dto;

public record ActivityFunnelDto(
        String stage,
        Long count,
        Double percentage
) {}
