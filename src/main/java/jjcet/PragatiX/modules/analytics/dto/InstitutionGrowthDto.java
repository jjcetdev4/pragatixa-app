package jjcet.PragatiX.modules.analytics.dto;

import java.time.LocalDate;

public record InstitutionGrowthDto(
        String period,
        Double overallGrowth,
        Double xpGrowth,
        Double attendance,
        Double engagement,
        Integer stage
) {}