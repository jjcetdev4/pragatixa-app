package com.pragatix.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_stage_mappings", uniqueConstraints = {
        @UniqueConstraint(name = "uq_stage_activity", columnNames = { "stage_id", "activity_id" })
})
public class ActivityStageMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stage_id", nullable = false)
    private ActivityStage stage;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subgroup_id", nullable = false)
    private ActivitySubgroup subgroup;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "award_xp")
    private Integer awardXp;

    @Column(name = "award_enabled")
    private Boolean awardEnabled;

    @Column(name = "penalty_enabled")
    private Boolean penaltyEnabled;

    @Column(name = "penalty_xp")
    private Integer penaltyXp;

    @Column(name = "award_frequency", length = 50)
    private String awardFrequency;

    @Column(name = "assignment_mode", length = 50)
    private String assignmentMode;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public ActivityStageMapping() {
    }

    public Integer getAwardXp() {
        return awardXp;
    }

    public void setAwardXp(Integer awardXp) {
        this.awardXp = awardXp;
    }

    public Boolean getAwardEnabled() {
        return awardEnabled;
    }

    public void setAwardEnabled(Boolean awardEnabled) {
        this.awardEnabled = awardEnabled;
    }

    public Boolean getPenaltyEnabled() {
        return penaltyEnabled;
    }

    public void setPenaltyEnabled(Boolean penaltyEnabled) {
        this.penaltyEnabled = penaltyEnabled;
    }

    public Integer getPenaltyXp() {
        return penaltyXp;
    }

    public void setPenaltyXp(Integer penaltyXp) {
        this.penaltyXp = penaltyXp;
    }

    public String getAwardFrequency() {
        return awardFrequency;
    }

    public void setAwardFrequency(String awardFrequency) {
        this.awardFrequency = awardFrequency;
    }

    public String getAssignmentMode() {
        return assignmentMode;
    }

    public void setAssignmentMode(String assignmentMode) {
        this.assignmentMode = assignmentMode;
    }

    public ActivityStageMapping(Activity activity, ActivityStage stage, ActivitySubgroup subgroup) {
        this.activity = activity;
        this.stage = stage;
        this.subgroup = subgroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public ActivityStage getStage() {
        return stage;
    }

    public void setStage(ActivityStage stage) {
        this.stage = stage;
    }

    public ActivitySubgroup getSubgroup() {
        return subgroup;
    }

    public void setSubgroup(ActivitySubgroup subgroup) {
        this.subgroup = subgroup;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
