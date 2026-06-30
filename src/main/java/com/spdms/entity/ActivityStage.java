package com.spdms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "activity_stages")
public class ActivityStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;

    public ActivityStage() {}

    public ActivityStage(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ActivityStage stage = new ActivityStage();
        public Builder name(String v) { stage.name = v; return this; }
        public Builder description(String v) { stage.description = v; return this; }
        public ActivityStage build() { return stage; }
    }
}
