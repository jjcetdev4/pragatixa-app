package jjcet.PragatiX.modules.analytics.dto;

public record StageDistributionDto(
        String stageName,
        Long studentCount,
        Double percentage
) {}