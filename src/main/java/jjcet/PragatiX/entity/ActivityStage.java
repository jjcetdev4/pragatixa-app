package jjcet.PragatiX.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import jjcet.PragatiX.enums.StageStatus;

@Entity
@Table(name = "activity_stages", uniqueConstraints = {
        @UniqueConstraint(name = "uq_stage_name", columnNames = { "stage_name" }),
        @UniqueConstraint(name = "UK94qv2sd8jwbxsmdv8r4aibi7v", columnNames = { "name" })
})
public class ActivityStage implements SoftDeletable {

    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanent_delete_at")
    private LocalDateTime permanentDeleteAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "expected_xp", nullable = false)
    private Integer expectedXp = 0;

    @Column(name = "start_datetime")
    private LocalDateTime startDateTime;

    @Column(name = "end_datetime")
    private LocalDateTime endDateTime;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StageStatus status = StageStatus.UPCOMING;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    @Column(name = "use_date_validation", nullable = false)
    private boolean useDateValidation = true;

    @Column(name = "use_threshold_validation", nullable = false)
    private boolean useThresholdValidation = false;

    @Column(name = "use_combined_validation", nullable = false)
    private boolean useCombinedValidation = false;

    @Column(name = "must_threshold", nullable = false)
    private Integer mustThreshold = 0;

    @Column(name = "individual_threshold", nullable = false)
    private Integer individualThreshold = 0;

    @Column(name = "group_threshold", nullable = false)
    private Integer groupThreshold = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "academic_year")
    private jjcet.PragatiX.enums.AcademicYear academicYear;

    public ActivityStage() {
    }

    public ActivityStage(Long id, String name, String description, Integer expectedXp, LocalDateTime startDateTime,
            LocalDateTime endDateTime, int displayOrder, StageStatus status) {
        this.id = id;
        this.name = name;
        this.stageName = name;
        this.description = description;
        this.expectedXp = expectedXp != null ? expectedXp : 0;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.displayOrder = displayOrder;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isUseDateValidation() {
        return useDateValidation;
    }

    public void setUseDateValidation(boolean useDateValidation) {
        this.useDateValidation = useDateValidation;
    }

    public boolean isUseThresholdValidation() {
        return useThresholdValidation;
    }

    public void setUseThresholdValidation(boolean useThresholdValidation) {
        this.useThresholdValidation = useThresholdValidation;
    }

    public boolean isUseCombinedValidation() {
        return useCombinedValidation;
    }

    public void setUseCombinedValidation(boolean useCombinedValidation) {
        this.useCombinedValidation = useCombinedValidation;
    }

    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getExpectedXp() {
        return expectedXp;
    }

    public void setExpectedXp(Integer expectedXp) {
        this.expectedXp = expectedXp;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public StageStatus getStatus() {
        return status;
    }

    public void setStatus(StageStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Integer getMustThreshold() {
        return mustThreshold;
    }

    public void setMustThreshold(Integer mustThreshold) {
        this.mustThreshold = mustThreshold;
    }

    public Integer getIndividualThreshold() {
        return individualThreshold;
    }

    public void setIndividualThreshold(Integer individualThreshold) {
        this.individualThreshold = individualThreshold;
    }

    public Integer getGroupThreshold() {
        return groupThreshold;
    }

    public void setGroupThreshold(Integer groupThreshold) {
        this.groupThreshold = groupThreshold;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public jjcet.PragatiX.enums.AcademicYear getAcademicYear() {
        return academicYear;
    }

    public void setAcademicYear(jjcet.PragatiX.enums.AcademicYear academicYear) {
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
        private final ActivityStage stage = new ActivityStage();

        public Builder id(Long v) {
            stage.id = v;
            return this;
        }

        public Builder stageName(String v) {
            stage.stageName = v;
            return this;
        }

        public Builder name(String v) {
            stage.name = v;
            return this;
        }

        public Builder description(String v) {
            stage.description = v;
            return this;
        }

        public Builder expectedXp(Integer v) {
            stage.expectedXp = v;
            return this;
        }

        public Builder startDateTime(LocalDateTime v) {
            stage.startDateTime = v;
            return this;
        }

        public Builder endDateTime(LocalDateTime v) {
            stage.endDateTime = v;
            return this;
        }

        public Builder displayOrder(int v) {
            stage.displayOrder = v;
            return this;
        }

        public Builder status(StageStatus v) {
            stage.status = v;
            return this;
        }

        public Builder isActive(boolean v) {
            stage.isActive = v;
            return this;
        }

        public Builder useDateValidation(boolean v) {
            stage.useDateValidation = v;
            return this;
        }

        public Builder useThresholdValidation(boolean v) {
            stage.useThresholdValidation = v;
            return this;
        }

        public Builder useCombinedValidation(boolean v) {
            stage.useCombinedValidation = v;
            return this;
        }

        public Builder mustThreshold(Integer v) {
            stage.mustThreshold = v;
            return this;
        }

        public Builder individualThreshold(Integer v) {
            stage.individualThreshold = v;
            return this;
        }

        public Builder groupThreshold(Integer v) {
            stage.groupThreshold = v;
            return this;
        }

        public Builder academicYear(jjcet.PragatiX.enums.AcademicYear v) {
            stage.academicYear = v;
            return this;
        }

        public Builder deleted(boolean v) {
            stage.deleted = v;
            return this;
        }

        public Builder deletedAt(LocalDateTime v) {
            stage.deletedAt = v;
            return this;
        }

        public Builder permanentDeleteAt(LocalDateTime v) {
            stage.permanentDeleteAt = v;
            return this;
        }

        public Builder deletedBy(String v) {
            stage.deletedBy = v;
            return this;
        }

        public ActivityStage build() {
            return stage;
        }
    }
}
