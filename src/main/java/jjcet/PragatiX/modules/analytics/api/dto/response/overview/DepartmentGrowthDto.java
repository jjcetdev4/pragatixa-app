package jjcet.PragatiX.modules.analytics.api.dto.response.overview;

public record DepartmentGrowthDto(
        Long departmentId,
        String departmentName,
        Double growthRate,
        Long totalXp
) {
}