package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record XpTopPerformerDto(
        Integer rank,
        String studentName,
        String registerNumber,
        String department,
        String section,
        Long currentXp,
        Long awardedXp,
        Long penaltyXp
) {
}