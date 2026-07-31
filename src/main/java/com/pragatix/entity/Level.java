package com.pragatix.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "levels")
public class Level {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_number", nullable = false, unique = true)
    private int levelNumber;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "xp_min", nullable = false)
    private int xpMin;

    @Column(name = "xp_max", nullable = false)
    private int xpMax;

    @Column(nullable = false)
    private int stage;

    @Column(name = "primary_objective", columnDefinition = "TEXT")
    private String primaryObjective;

    @Column(name = "key_unlocks", columnDefinition = "TEXT")
    private String keyUnlocks;

    public Level() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public void setLevelNumber(int levelNumber) {
        this.levelNumber = levelNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getXpMin() {
        return xpMin;
    }

    public void setXpMin(int xpMin) {
        this.xpMin = xpMin;
    }

    public int getXpMax() {
        return xpMax;
    }

    public void setXpMax(int xpMax) {
        this.xpMax = xpMax;
    }

    public int getStage() {
        return stage;
    }

    public void setStage(int stage) {
        this.stage = stage;
    }

    public String getPrimaryObjective() {
        return primaryObjective;
    }

    public void setPrimaryObjective(String primaryObjective) {
        this.primaryObjective = primaryObjective;
    }

    public String getKeyUnlocks() {
        return keyUnlocks;
    }

    public void setKeyUnlocks(String keyUnlocks) {
        this.keyUnlocks = keyUnlocks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Level level = new Level();

        public Builder levelNumber(int v) {
            level.levelNumber = v;
            return this;
        }

        public Builder title(String v) {
            level.title = v;
            return this;
        }

        public Builder xpMin(int v) {
            level.xpMin = v;
            return this;
        }

        public Builder xpMax(int v) {
            level.xpMax = v;
            return this;
        }

        public Builder stage(int v) {
            level.stage = v;
            return this;
        }

        public Builder primaryObjective(String v) {
            level.primaryObjective = v;
            return this;
        }

        public Builder keyUnlocks(String v) {
            level.keyUnlocks = v;
            return this;
        }

        public Level build() {
            return level;
        }
    }
}
