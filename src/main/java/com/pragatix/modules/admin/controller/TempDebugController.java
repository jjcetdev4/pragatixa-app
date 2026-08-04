package com.pragatix.modules.admin.controller;

import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityStageMapping;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.activity.repository.ActivityStageMappingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class TempDebugController {

    private final ActivityRepository activityRepository;
    private final ActivityStageMappingRepository mappingRepository;

    public TempDebugController(ActivityRepository activityRepository, ActivityStageMappingRepository mappingRepository) {
        this.activityRepository = activityRepository;
        this.mappingRepository = mappingRepository;
    }

    @GetMapping("/api/v1/public/debug/attendance-activity")
    public String debug() {
        StringBuilder sb = new StringBuilder();
        
        List<Activity> activities = activityRepository.findAll().stream()
            .filter(a -> Boolean.TRUE.equals(a.getAttendanceEngineEnabled()))
            .collect(Collectors.toList());
            
        sb.append("1. Database - Attendance Activities\n");
        for (Activity a : activities) {
            sb.append("Activity ID : ").append(a.getId()).append("\n");
            sb.append("Activity Name : ").append(a.getName()).append("\n");
            sb.append("Activity Status : ").append(a.getStatus()).append("\n");
            sb.append("Stage ID : ").append(a.getStage() != null ? a.getStage().getId() : "null").append("\n");
            sb.append("Subgroup ID : ").append(a.getSubgroup() != null ? a.getSubgroup().getId() : "null").append("\n");
            sb.append("Subgroup Name : ").append(a.getSubgroup() != null ? a.getSubgroup().getName() : "null").append("\n");
            sb.append("Category : ").append(a.getSubgroup() != null ? a.getSubgroup().getCategory() : "null").append("\n");
            sb.append("Mode Type : ").append(a.getModeType()).append("\n");
            sb.append("Mandatory Flag : ").append(a.isMandatory()).append("\n");
            sb.append("Attendance Engine Enabled : ").append(a.getAttendanceEngineEnabled()).append("\n\n");
            
            sb.append("2. Stage Mappings for Activity ID ").append(a.getId()).append("\n");
            List<ActivityStageMapping> mappings = mappingRepository.findAll().stream()
                .filter(m -> m.getActivity() != null && m.getActivity().getId().equals(a.getId()))
                .collect(Collectors.toList());
                
            for (ActivityStageMapping m : mappings) {
                sb.append("mapping_id : ").append(m.getId()).append("\n");
                sb.append("activity_id : ").append(m.getActivity().getId()).append("\n");
                sb.append("stage_id : ").append(m.getStage() != null ? m.getStage().getId() : "null").append("\n");
                sb.append("subgroup_id : ").append(m.getSubgroup() != null ? m.getSubgroup().getId() : "null").append("\n");
                sb.append("subgroup_name : ").append(m.getSubgroup() != null ? m.getSubgroup().getName() : "null").append("\n");
                sb.append("subgroup_category : ").append(m.getSubgroup() != null ? m.getSubgroup().getCategory() : "null").append("\n\n");
            }
        }
        
        return sb.toString();
    }
}
