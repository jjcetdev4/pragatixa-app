package com.spdms.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_stages", uniqueConstraints = {
    @UniqueConstraint(name = "uq_stage_name", columnNames = {"stage_name"}),
    @UniqueConstraint(name = "UK94qv2sd8jwbxsmdv8r4aibi7v", columnNames = {"name"})
})
public class ActivityStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stage_name", nullable = false, length = 100)
    private String stageName;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public ActivityStage() {}

    public ActivityStage(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ActivityStage stage = new ActivityStage();
        public Builder stageName(String v) { stage.stageName = v; return this; }
        public Builder name(String v) { stage.name = v; return this; }
        public Builder description(String v) { stage.description = v; return this; }
        public ActivityStage build() { return stage; }
    }
}
