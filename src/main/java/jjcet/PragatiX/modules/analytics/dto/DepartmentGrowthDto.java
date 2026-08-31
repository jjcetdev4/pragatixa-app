package jjcet.PragatiX.modules.analytics.dto;

public record DepartmentGrowthDto(
        Long departmentId,
        String departmentName,
        Double growthRate,
        Long totalXp
) {}