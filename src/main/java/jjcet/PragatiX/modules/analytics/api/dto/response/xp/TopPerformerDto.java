package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record TopPerformerDto(
        String registerNumber,
        String fullName,
        String department,
        Integer totalXp,
        Integer currentStage,
        Long badgesEarned,
        Integer currentStreak,
        Long rank
) {
}