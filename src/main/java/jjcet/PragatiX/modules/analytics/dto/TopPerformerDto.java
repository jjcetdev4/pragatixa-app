package jjcet.PragatiX.modules.analytics.dto;

public class TopPerformerDto {
    private String regNo;
    private String fullName;
    private String department;
    private Integer totalXp;
    private Integer currentStage;
    private Long badgesEarned;
    private Integer currentStreak;
    private Long rankPosition;

    public TopPerformerDto() {
    }

    public TopPerformerDto(String regNo, String fullName, String department,
                          Integer totalXp, Integer currentStage, Long badgesEarned,
                          Integer currentStreak, Long rankPosition) {
        this.regNo = regNo;
        this.fullName = fullName;
        this.department = department;
        this.totalXp = totalXp;
        this.currentStage = currentStage;
        this.badgesEarned = badgesEarned;
        this.currentStreak = currentStreak;
        this.rankPosition = rankPosition;
    }

    public String getRegNo() {
        return regNo;
    }

    public void setRegNo(String regNo) {
        this.regNo = regNo;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(Integer totalXp) {
        this.totalXp = totalXp;
    }

    public Integer getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(Integer currentStage) {
        this.currentStage = currentStage;
    }

    public Long getBadgesEarned() {
        return badgesEarned;
    }

    public void setBadgesEarned(Long badgesEarned) {
        this.badgesEarned = badgesEarned;
    }

    public Integer getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public Long getRankPosition() {
        return rankPosition;
    }

    public void setRankPosition(Long rankPosition) {
        this.rankPosition = rankPosition;
    }
}