package jjcet.PragatiX.modules.admin.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jjcet.PragatiX.enums.AcademicYear;

public class LevelCreateUpdateDto {

    @Min(value = 1, message = "Level number must be at least 1")
    private int levelNumber;

    @NotBlank(message = "Title is required")
    private String title;

    @Min(value = 0, message = "Minimum XP cannot be negative")
    private int xpMin;

    @Min(value = 1, message = "Maximum XP must be greater than 0")
    private int xpMax;

    @Min(value = 1, message = "Stage must be at least 1")
    private int stage = 1;

    private String primaryObjective;

    private String keyUnlocks;

    private String academicYear;

    public LevelCreateUpdateDto() {
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
