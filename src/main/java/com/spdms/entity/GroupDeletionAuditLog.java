package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "group_deletion_audit_log")
public class GroupDeletionAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "team_name", nullable = false, length = 255)
    private String teamName;

    @Column(name = "deleted_by_user_id", nullable = false, length = 100)
    private String deletedByUserId;

    @Column(name = "deleted_by_role", nullable = false, length = 100)
    private String deletedByRole;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public GroupDeletionAuditLog() {
    }

    public GroupDeletionAuditLog(Long teamId, String teamName, String deletedByUserId, String deletedByRole,
            String reason, LocalDateTime timestamp) {
        this.teamId = teamId;
        this.teamName = teamName;
        this.deletedByUserId = deletedByUserId;
        this.deletedByRole = deletedByRole;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getDeletedByUserId() {
        return deletedByUserId;
    }

    public void setDeletedByUserId(String deletedByUserId) {
        this.deletedByUserId = deletedByUserId;
    }

    public String getDeletedByRole() {
        return deletedByRole;
    }

    public void setDeletedByRole(String deletedByRole) {
        this.deletedByRole = deletedByRole;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
