package jjcet.PragatiX.dto.response;

import jjcet.PragatiX.entity.AuditLog;
import java.time.LocalDateTime;

public class AuditLogDTO {
    private Long id;
    private Long actorUserId;
    private String actorName;
    private String actorRole;
    private String action;
    private String module;
    private String entityType;
    private Long entityId;
    private String description;
    private String oldValues;
    private String newValues;
    private LocalDateTime createdAt;

    public AuditLogDTO() {}

    public AuditLogDTO(AuditLog log) {
        this.id = log.getId();
        this.actorUserId = log.getActorUserId();
        this.actorName = log.getActorName();
        this.actorRole = log.getActorRole();
        this.action = log.getAction().name();
        this.module = log.getModule().name();
        this.entityType = log.getEntityType();
        this.entityId = log.getEntityId();
        this.description = log.getDescription();
        this.oldValues = log.getOldValues();
        this.newValues = log.getNewValues();
        this.createdAt = log.getCreatedAt();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long actorUserId) { this.actorUserId = actorUserId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getActorRole() { return actorRole; }
    public void setActorRole(String actorRole) { this.actorRole = actorRole; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public Long getEntityId() { return entityId; }
    public void setEntityId(Long entityId) { this.entityId = entityId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getOldValues() { return oldValues; }
    public void setOldValues(String oldValues) { this.oldValues = oldValues; }
    public String getNewValues() { return newValues; }
    public void setNewValues(String newValues) { this.newValues = newValues; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
