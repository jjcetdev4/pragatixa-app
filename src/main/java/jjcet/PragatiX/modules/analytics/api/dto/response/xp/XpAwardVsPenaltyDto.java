package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record XpAwardVsPenaltyDto(
        String department,
        Long awardXp,
        Long penaltyXp
) {
}