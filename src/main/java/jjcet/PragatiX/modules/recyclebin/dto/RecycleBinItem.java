package jjcet.PragatiX.modules.recyclebin.dto;

import java.time.LocalDateTime;

public class RecycleBinItem {
    private Long id;
    private String entityType; // e.g., "USER", "FACULTY", "STUDENT", "ACTIVITY", "TEAM"
    private String entityName; // e.g., Username, Full Name, Activity Name, Team Name
    private LocalDateTime deletedAt;
    private LocalDateTime permanentDeleteAt;
    private String deletedBy;
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

    public Object getDetails() {
        return details;
    }

    public void setDetails(Object details) {
        this.details = details;
    }
}
