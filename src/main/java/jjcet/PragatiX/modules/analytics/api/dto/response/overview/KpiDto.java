package jjcet.PragatiX.modules.analytics.api.dto.response.overview;

public record KpiDto(
        Long totalStudents,
        Double totalXp,
        Double avgXp,
        Double attendance,
        Long atRiskStudents
) {
}