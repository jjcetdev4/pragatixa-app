package jjcet.PragatiX.modules.analytics.dto;

public record ActivityFunnelStage(
        String stage,
        Long count,
        Double percentage
) {}
