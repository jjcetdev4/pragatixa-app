package com.spdms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "activity_subgroups")
public class ActivitySubgroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int threshold;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stage_id", nullable = false)
    private ActivityStage stage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_faculty_id")
    private User assignedFaculty;

    public ActivitySubgroup() {}

    public ActivitySubgroup(Long id, String name, int threshold, ActivityStage stage, User assignedFaculty) {
        this.id = id;
        this.name = name;
        this.threshold = threshold;
        this.stage = stage;
        this.assignedFaculty = assignedFaculty;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }

    public ActivityStage getStage() { return stage; }
    public void setStage(ActivityStage stage) { this.stage = stage; }

    public User getAssignedFaculty() { return assignedFaculty; }
    public void setAssignedFaculty(User assignedFaculty) { this.assignedFaculty = assignedFaculty; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final ActivitySubgroup subgroup = new ActivitySubgroup();
        public Builder name(String v) { subgroup.name = v; return this; }
        public Builder threshold(int v) { subgroup.threshold = v; return this; }
        public Builder stage(ActivityStage v) { subgroup.stage = v; return this; }
        public Builder assignedFaculty(User v) { subgroup.assignedFaculty = v; return this; }
        public ActivitySubgroup build() { return subgroup; }
    }
}
