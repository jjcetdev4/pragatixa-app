package jjcet.PragatiX.modules.admin.dto.response;

import jjcet.PragatiX.entity.Level;

public class LevelDto {
    private Long id;
    private int levelNumber;
    private String title;
    private int xpMin;
    private int xpMax;
    private int stage;
    private String primaryObjective;
    private String keyUnlocks;
    private String academicYear;

    public LevelDto() {
    }

    public LevelDto(Long id, int levelNumber, String title, int xpMin, int xpMax, int stage,
                    String primaryObjective, String keyUnlocks, String academicYear) {
        this.id = id;
        this.levelNumber = levelNumber;
        this.title = title;
        this.xpMin = xpMin;
        this.xpMax = xpMax;
        this.stage = stage;
        this.primaryObjective = primaryObjective;
        this.keyUnlocks = keyUnlocks;
        this.academicYear = academicYear;
    }

    public static LevelDto fromEntity(Level level) {
        if (level == null) return null;
        return new LevelDto(
                level.getId(),
                level.getLevelNumber(),
                level.getTitle(),
                level.getXpMin(),
                level.getXpMax(),
                level.getStage(),
                level.getPrimaryObjective(),
                level.getKeyUnlocks(),
                level.getAcademicYear() != null ? level.getAcademicYear().name() : null
        );
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

    public String getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(String academicYear) {
        this.academicYear = academicYear;
    }
}
