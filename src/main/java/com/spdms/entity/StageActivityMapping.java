package com.spdms.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stage_activity_mappings")
public class StageActivityMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "stage_id", nullable = false)
    private ActivityStage stage;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(name = "subgroup_type", nullable = false, length = 20)
    private String subgroupType;

    public StageActivityMapping() {}

    public StageActivityMapping(Activity activity, ActivityStage stage, String subgroupType) {
        this.activity = activity;
        this.stage = stage;
        this.subgroupType = subgroupType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Activity getActivity() { return activity; }
    public void setActivity(Activity activity) { this.activity = activity; }

    public ActivityStage getStage() { return stage; }
    public void setStage(ActivityStage stage) { this.stage = stage; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public String getSubgroupType() { return subgroupType; }
    public void setSubgroupType(String subgroupType) { this.subgroupType = subgroupType; }
}
