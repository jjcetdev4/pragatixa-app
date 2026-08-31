package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record GroupedXpDto(
        String groupName,
        Double averageXp,
        Long totalXp,
        Long studentCount
) {
}