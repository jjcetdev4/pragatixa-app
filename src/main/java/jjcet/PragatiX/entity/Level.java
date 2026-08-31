package jjcet.PragatiX.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jjcet.PragatiX.enums.AcademicYear;

@Entity
@Table(name = "levels")
public class Level implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "level_number", nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_year", length = 30)
    private AcademicYear academicYear;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanent_delete_at")
    private LocalDateTime permanentDeleteAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

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

    public AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(AcademicYear academicYear) {
        this.academicYear = academicYear;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    @Override
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public LocalDateTime getPermanentDeleteAt() {
        return permanentDeleteAt;
    }

    @Override
    public void setPermanentDeleteAt(LocalDateTime permanentDeleteAt) {
        this.permanentDeleteAt = permanentDeleteAt;
    }

    @Override
    public String getDeletedBy() {
        return deletedBy;
    }

    @Override
    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
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

        public Builder academicYear(AcademicYear v) {
            level.academicYear = v;
            return this;
        }

        public Builder deleted(boolean v) {
            level.deleted = v;
            return this;
        }

        public Level build() {
            return level;
        }
    }
}
