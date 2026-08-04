package com.pragatix.modules.analytics.dto;

import java.time.LocalDate;

public class XpHeatmapDTO {
    private LocalDate date;
    private Long xp;
    private Integer level;

    public XpHeatmapDTO() {}

    public XpHeatmapDTO(LocalDate date, Long xp, Integer level) {
        this.date = date;
        this.xp = xp;
        this.level = level;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public Long getXp() { return xp; }
    public void setXp(Long xp) { this.xp = xp; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
}
