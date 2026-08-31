package jjcet.PragatiX.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_categories", indexes = {
        @Index(name = "idx_activity_cat_name", columnList = "name"),
        @Index(name = "idx_activity_cat_deleted", columnList = "deleted")
})
public class ActivityCategory implements SoftDeletable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "activity_name", length = 100)
    private String activityName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String icon;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "permanent_delete_at")
    private LocalDateTime permanentDeleteAt;

    @Column(name = "deleted_by", length = 100)
    private String deletedBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ActivityCategory() {
    }

    public ActivityCategory(String name, String description) {
        this.name = name;
        this.activityName = name;
        this.description = description;
        this.deleted = false;
        this.displayOrder = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.activityName = name;
    }

    public String getActivityName() {
        return activityName != null ? activityName : name;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
        if (this.name == null) {
            this.name = activityName;
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String description;
        private String icon;
        private Integer displayOrder = 0;
        private boolean deleted = false;
        private LocalDateTime deletedAt;
        private LocalDateTime permanentDeleteAt;
        private String deletedBy;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder icon(String icon) {
            this.icon = icon;
            return this;
        }

        public Builder displayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
            return this;
        }

        public Builder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder deletedAt(LocalDateTime deletedAt) {
            this.deletedAt = deletedAt;
            return this;
        }

        public Builder permanentDeleteAt(LocalDateTime permanentDeleteAt) {
            this.permanentDeleteAt = permanentDeleteAt;
            return this;
        }

        public Builder deletedBy(String deletedBy) {
            this.deletedBy = deletedBy;
            return this;
        }

        public ActivityCategory build() {
            ActivityCategory cat = new ActivityCategory();
            cat.setId(this.id);
            cat.setName(this.name);
            cat.setActivityName(this.name);
            cat.setDescription(this.description);
            cat.setIcon(this.icon);
            cat.setDisplayOrder(this.displayOrder);
            cat.setDeleted(this.deleted);
            cat.setDeletedAt(this.deletedAt);
            cat.setPermanentDeleteAt(this.permanentDeleteAt);
            cat.setDeletedBy(this.deletedBy);
            return cat;
        }
    }
}
