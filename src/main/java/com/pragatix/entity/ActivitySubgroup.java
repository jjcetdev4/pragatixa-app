package com.pragatix.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "activity_subgroups")
public class ActivitySubgroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String category;

    @Column(nullable = false, length = 100)
    private String name;

    @Deprecated
    @Column(name = "threshold", nullable = false)
    private int threshold;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stage_id", nullable = false)
    private ActivityStage stage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_faculty_id")
    private User assignedFaculty;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assigned_department_id")
    private Department assignedDepartment;

    public ActivitySubgroup() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Deprecated
    public int getThreshold() {
        return threshold;
    }

    @Deprecated
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public ActivityStage getStage() {
        return stage;
    }

    public void setStage(ActivityStage stage) {
        this.stage = stage;
    }

    public User getAssignedFaculty() {
        return assignedFaculty;
    }

    public void setAssignedFaculty(User assignedFaculty) {
        this.assignedFaculty = assignedFaculty;
    }

    public Department getAssignedDepartment() {
        return assignedDepartment;
    }

    public void setAssignedDepartment(Department assignedDepartment) {
        this.assignedDepartment = assignedDepartment;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ActivitySubgroup subgroup = new ActivitySubgroup();

        public Builder category(String v) {
            subgroup.category = v;
            return this;
        }

        public Builder name(String v) {
            subgroup.name = v;
            return this;
        }

        public Builder threshold(int v) {
            subgroup.threshold = v;
            return this;
        }

        public Builder stage(ActivityStage v) {
            subgroup.stage = v;
            return this;
        }

        public Builder assignedFaculty(User v) {
            subgroup.assignedFaculty = v;
            return this;
        }

        public Builder assignedDepartment(Department v) {
            subgroup.assignedDepartment = v;
            return this;
        }

        public ActivitySubgroup build() {
            return subgroup;
        }
    }
}
