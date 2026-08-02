package com.pragatix.modules.admin.controller;

import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityStageMapping;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.activity.repository.ActivityStageMappingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
public class DebugDataTest {

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityStageMappingRepository mappingRepository;

    @Test
    public void dumpAttendanceEngineActivities() {
        System.out.println("======================================");
        System.out.println("START DEBUG DUMP");
        System.out.println("======================================");
        
        List<Activity> activities = activityRepository.findAll().stream()
            .filter(a -> Boolean.TRUE.equals(a.getAttendanceEngineEnabled()))
            .collect(Collectors.toList());
            
        System.out.println("1. Database - Attendance Activities");
        for (Activity a : activities) {
            System.out.println("Activity ID : " + a.getId());
            System.out.println("Activity Name : " + a.getName());
            System.out.println("Activity Status : " + a.getStatus());
            System.out.println("Stage ID : " + (a.getStage() != null ? a.getStage().getId() : "null"));
            System.out.println("Subgroup ID : " + (a.getSubgroup() != null ? a.getSubgroup().getId() : "null"));
            System.out.println("Subgroup Name : " + (a.getSubgroup() != null ? a.getSubgroup().getName() : "null"));
            System.out.println("Category : " + (a.getSubgroup() != null ? a.getSubgroup().getCategory() : "null"));
            System.out.println("Mode Type : " + a.getModeType());
            System.out.println("Mandatory Flag : " + a.isMandatory());
            System.out.println("Attendance Engine Enabled : " + a.getAttendanceEngineEnabled());
            System.out.println();
            
            System.out.println("2. Stage Mappings for Activity ID " + a.getId());
            List<ActivityStageMapping> mappings = mappingRepository.findAll().stream()
                .filter(m -> m.getActivity() != null && m.getActivity().getId().equals(a.getId()))
                .collect(Collectors.toList());
                
            for (ActivityStageMapping m : mappings) {
                System.out.println("mapping_id : " + m.getId());
                System.out.println("activity_id : " + m.getActivity().getId());
                System.out.println("stage_id : " + (m.getStage() != null ? m.getStage().getId() : "null"));
                System.out.println("subgroup_id : " + (m.getSubgroup() != null ? m.getSubgroup().getId() : "null"));
                System.out.println("subgroup_name : " + (m.getSubgroup() != null ? m.getSubgroup().getName() : "null"));
                System.out.println("subgroup_category : " + (m.getSubgroup() != null ? m.getSubgroup().getCategory() : "null"));
                System.out.println();
            }
        }
        
        System.out.println("======================================");
        System.out.println("END DEBUG DUMP");
        System.out.println("======================================");
    }
}
