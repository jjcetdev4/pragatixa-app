package jjcet.PragatiX.modules.analytics.dto;

public class EliteTeamDto {
    private Long teamId;
    private String teamName;
    private String department;
    private Integer teamSize;
    private Long totalTeamXp;
    private Double avgTeamXp;
    private Long groupActivityXp;
    private Long badgesEarnedByTeam;

    public EliteTeamDto() {
    }

    public EliteTeamDto(Long teamId, String teamName, String department,
                       Integer teamSize, Long totalTeamXp, Double avgTeamXp,
                       Long groupActivityXp, Long badgesEarnedByTeam) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.department = department;
        this.teamSize = teamSize;
        this.totalTeamXp = totalTeamXp;
        this.avgTeamXp = avgTeamXp;
        this.groupActivityXp = groupActivityXp;
        this.badgesEarnedByTeam = badgesEarnedByTeam;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getTeamSize() {
        return teamSize;
    }

    public void setTeamSize(Integer teamSize) {
        this.teamSize = teamSize;
    }

    public Long getTotalTeamXp() {
        return totalTeamXp;
    }

    public void setTotalTeamXp(Long totalTeamXp) {
        this.totalTeamXp = totalTeamXp;
    }

    public Double getAvgTeamXp() {
        return avgTeamXp;
    }

    public void setAvgTeamXp(Double avgTeamXp) {
        this.avgTeamXp = avgTeamXp;
    }

    public Long getGroupActivityXp() {
        return groupActivityXp;
    }

    public void setGroupActivityXp(Long groupActivityXp) {
        this.groupActivityXp = groupActivityXp;
    }

    public Long getBadgesEarnedByTeam() {
        return badgesEarnedByTeam;
    }

    public void setBadgesEarnedByTeam(Long badgesEarnedByTeam) {
        this.badgesEarnedByTeam = badgesEarnedByTeam;
    }
}