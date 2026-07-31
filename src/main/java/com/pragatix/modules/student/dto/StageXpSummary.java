package com.pragatix.modules.student.dto;

public class StageXpSummary {
    private int totalXp;
    private int mustXp;
    private int individualXp;
    private int groupXp;

    public StageXpSummary() {
    }

    public StageXpSummary(int totalXp, int mustXp, int individualXp, int groupXp) {
        this.totalXp = totalXp;
        this.mustXp = mustXp;
        this.individualXp = individualXp;
        this.groupXp = groupXp;
    }

    public int getTotalXp() {
        return totalXp;
    }

    public void setTotalXp(int totalXp) {
        this.totalXp = totalXp;
    }

    public int getMustXp() {
        return mustXp;
    }

    public void setMustXp(int mustXp) {
        this.mustXp = mustXp;
    }

    public int getIndividualXp() {
        return individualXp;
    }

    public void setIndividualXp(int individualXp) {
        this.individualXp = individualXp;
    }

    public int getGroupXp() {
        return groupXp;
    }

    public void setGroupXp(int groupXp) {
        this.groupXp = groupXp;
    }
}
