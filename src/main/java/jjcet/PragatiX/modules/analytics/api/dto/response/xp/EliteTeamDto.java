package jjcet.PragatiX.modules.analytics.api.dto.response.xp;

public record EliteTeamDto(
        Long teamId,
        String teamName,
        String department,
        Integer teamSize,
        Long totalTeamXp,
        Double avgTeamXp,
        Long groupActivityXp,
        Long badgesEarnedByTeam
) {
}