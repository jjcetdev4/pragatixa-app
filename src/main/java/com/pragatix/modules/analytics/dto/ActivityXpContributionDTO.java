package com.pragatix.modules.analytics.dto;

public class ActivityXpContributionDTO {
    private String activityName;
    private String category;
    private Long totalAwardXp;
    private Long totalPenaltyXp;
    private Long netXp;

    public ActivityXpContributionDTO() {}

    public ActivityXpContributionDTO(String activityName, String category, Long totalAwardXp, Long totalPenaltyXp, Long netXp) {
        this.activityName = activityName;
        this.category = category;
        this.totalAwardXp = totalAwardXp;
        this.totalPenaltyXp = totalPenaltyXp;
        this.netXp = netXp;
    }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Long getTotalAwardXp() { return totalAwardXp; }
    public void setTotalAwardXp(Long totalAwardXp) { this.totalAwardXp = totalAwardXp; }

    public Long getTotalPenaltyXp() { return totalPenaltyXp; }
    public void setTotalPenaltyXp(Long totalPenaltyXp) { this.totalPenaltyXp = totalPenaltyXp; }

    public Long getNetXp() { return netXp; }
    public void setNetXp(Long netXp) { this.netXp = netXp; }
}
