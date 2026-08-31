package jjcet.PragatiX.modules.recyclebin.dto;

import java.time.LocalDateTime;

public class RecycleBinItem {
    private Long id;
    private String entityType; // e.g., "USER", "FACULTY", "STUDENT", "ACTIVITY", "TEAM", "ACTIVITY_CATEGORY", "ACTIVITY_EVIDENCE", "LEVEL", "BADGE", "DEPARTMENT"
    private String entityName; // e.g., Username, Full Name, Activity Name, Team Name
    private LocalDateTime deletedAt;
    private LocalDateTime permanentDeleteAt;
    private String deletedBy;
    private String originalLocation;
    private String description;
    private Object details; // Optional: can contain a summary object

    public RecycleBinItem() {}

    public RecycleBinItem(Long id, String entityType, String entityName, LocalDateTime deletedAt, LocalDateTime permanentDeleteAt, String deletedBy) {
        this.id = id;
        this.entityType = entityType;
        this.entityName = entityName;
        this.deletedAt = deletedAt;
        this.permanentDeleteAt = permanentDeleteAt;
        this.deletedBy = deletedBy;
    }

    public RecycleBinItem(Long id, String entityType, String entityName, LocalDateTime deletedAt, LocalDateTime permanentDeleteAt, String deletedBy, String originalLocation, String description) {
        this.id = id;
        this.entityType = entityType;
        this.entityName = entityName;
        this.deletedAt = deletedAt;
        this.permanentDeleteAt = permanentDeleteAt;
        this.deletedBy = deletedBy;
        this.originalLocation = originalLocation;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public LocalDateTime getPermanentDeleteAt() {
        return permanentDeleteAt;
    }

    public void setPermanentDeleteAt(LocalDateTime permanentDeleteAt) {
        this.permanentDeleteAt = permanentDeleteAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    public void setDeletedBy(String deletedBy) {
        this.deletedBy = deletedBy;
    }

    public String getOriginalLocation() {
        return originalLocation;
    }

    public void setOriginalLocation(String originalLocation) {
        this.originalLocation = originalLocation;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }
}

