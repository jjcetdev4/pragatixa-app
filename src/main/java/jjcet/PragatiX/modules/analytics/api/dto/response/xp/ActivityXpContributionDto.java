package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record ActivityXpContributionDto(
        String activityName,
        String category,
        Long awardXp,
        Long penaltyXp,
        Long netXp
) {
}