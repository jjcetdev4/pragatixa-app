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
@org.springframework.transaction.annotation.Transactional
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

    @Test
    public void dumpNeopatAndActivities() {
        System.out.println("================== ALL ACTIVITIES ====================");
        List<Activity> all = activityRepository.findAll();
        for (Activity a : all) {
            System.out.println("ACT ID: " + a.getId() + " | Name: " + a.getName() + " / " + a.getActivityName()
                    + " | Stage: " + (a.getStage() != null ? ("ID:" + a.getStage().getId() + ", Order:" + a.getStage().getDisplayOrder() + ", Name:" + a.getStage().getStageName()) : "null")
                    + " | Subgroup: " + (a.getSubgroup() != null ? a.getSubgroup().getName() : "null")
                    + " | Subgroup Stage: " + (a.getSubgroup() != null && a.getSubgroup().getStage() != null ? ("ID:" + a.getSubgroup().getStage().getId() + ", Order:" + a.getSubgroup().getStage().getDisplayOrder()) : "null"));
            List<ActivityStageMapping> maps = mappingRepository.findAll().stream()
                    .filter(m -> m.getActivity() != null && m.getActivity().getId().equals(a.getId()))
                    .collect(Collectors.toList());
            for (ActivityStageMapping m : maps) {
                System.out.println("   --> MAPPING: Stage ID=" + (m.getStage() != null ? m.getStage().getId() : "null")
                        + ", DisplayOrder=" + (m.getStage() != null ? m.getStage().getDisplayOrder() : "null")
                        + ", StageName=" + (m.getStage() != null ? m.getStage().getStageName() : "null")
                        + ", Subgroup=" + (m.getSubgroup() != null ? m.getSubgroup().getName() : "null"));
            }
        }
        System.out.println("======================================================");
    }

    @Autowired
    private com.pragatix.repository.ActivityAssignmentRepository assignmentRepository;

    @Test
    public void dumpNeopatAssignments() {
        System.out.println("================== NEOPAT ASSIGNMENTS ====================");
        List<com.pragatix.entity.ActivityAssignment> assigns = assignmentRepository.findByActivityId(33L);
        for (com.pragatix.entity.ActivityAssignment a : assigns) {
            System.out.println("ASSIGN ID: " + a.getId()
                    + " | Faculty: " + (a.getTeacher() != null ? a.getTeacher().getFullName() : "null")
                    + " | Stage: " + (a.getStage() != null ? ("ID:" + a.getStage().getId() + ", Order:" + a.getStage().getDisplayOrder() + ", Name:" + a.getStage().getStageName()) : "null")
                    + " | Year: " + a.getYear()
                    + " | Dept: " + (a.getDepartment() != null ? a.getDepartment().getId() : "null")
                    + " | Sec: " + (a.getSection() != null ? a.getSection().getId() : "null"));
        }
    }

    @Autowired
    private com.pragatix.modules.student.service.StudentActivityQueryService studentActivityQueryService;

    @Autowired
    private com.pragatix.modules.student.repository.StudentRepository studentRepository;

    @Test
    public void testNeopatStageEligibilityFiltering() {
        System.out.println("================== TESTING NEOPAT STAGE ELIGIBILITY ====================");
        // Neopat activity ID = 33, Teacher = "jaga", Year = "1", Dept = 8, Section = 47
        // Stage 2 (ID = 28, DisplayOrder = 2)
        var responseStage2 = studentActivityQueryService.getStudentsForActivity(33L, "1", 8L, 47L, "jaga", 28L);
        org.junit.jupiter.api.Assertions.assertNotNull(responseStage2);
        org.junit.jupiter.api.Assertions.assertNotNull(responseStage2.getBody());
        org.junit.jupiter.api.Assertions.assertNotNull(responseStage2.getBody().getData());
        var studentsStage2 = responseStage2.getBody().getData().getStudents();
        System.out.println("Stage 2 returned students count: " + studentsStage2.size());
        org.junit.jupiter.api.Assertions.assertTrue(studentsStage2.size() > 0, "Stage 2 should return eligible students");
        for (var s : studentsStage2) {
            var studentEntity = studentRepository.findById(s.getId()).orElse(null);
            org.junit.jupiter.api.Assertions.assertNotNull(studentEntity);
            int stg = studentEntity.getCurrentStage() > 0 ? studentEntity.getCurrentStage() : studentEntity.getStage();
            System.out.println("Stage 2 Student: " + studentEntity.getFullName() + " | Stage: " + stg);
            org.junit.jupiter.api.Assertions.assertEquals(2, stg, "Student must belong to Stage 2");
        }

        // Stage 1 (ID = 27, DisplayOrder = 1)
        var responseStage1 = studentActivityQueryService.getStudentsForActivity(33L, "1", 8L, 47L, "jaga", 27L);
        org.junit.jupiter.api.Assertions.assertNotNull(responseStage1);
        org.junit.jupiter.api.Assertions.assertNotNull(responseStage1.getBody());
        org.junit.jupiter.api.Assertions.assertNotNull(responseStage1.getBody().getData());
        var studentsStage1 = responseStage1.getBody().getData().getStudents();
        System.out.println("Stage 1 returned students count: " + studentsStage1.size());
        org.junit.jupiter.api.Assertions.assertTrue(studentsStage1.size() > 0, "Stage 1 should return eligible students");
        for (var s : studentsStage1) {
            var studentEntity = studentRepository.findById(s.getId()).orElse(null);
            org.junit.jupiter.api.Assertions.assertNotNull(studentEntity);
            int stg = studentEntity.getCurrentStage() > 0 ? studentEntity.getCurrentStage() : studentEntity.getStage();
            System.out.println("Stage 1 Student: " + studentEntity.getFullName() + " | Stage: " + stg);
            org.junit.jupiter.api.Assertions.assertEquals(1, stg, "Student must belong to Stage 1");
        }
        System.out.println("=========================================================================");
    }

    @Autowired
    private com.pragatix.repository.TeamRepository teamRepo;

    @Autowired
    private com.pragatix.repository.StageTeamRepository stageTeamRepo;

    @Test
    public void dumpTeamsAndAssignments() {
        System.out.println("================== DUMPING TEAMS AND STAGETEAMS ====================");
        List<com.pragatix.entity.Team> teams = teamRepo.findAll();
        for (var t : teams) {
            List<com.pragatix.entity.StageTeam> stList = stageTeamRepo.findByTeamId(t.getId());
            String stInfo = stList.stream().map(st -> "StageTeam[ID:" + st.getId() + ", Stage:" + (st.getStage() != null ? st.getStage().getDisplayOrder() + "(" + st.getStage().getStageName() + ")" : "null") + "]").collect(Collectors.joining(", "));
            System.out.println("TEAM ID: " + t.getId() + " | Name: '" + t.getName() + "' | Dept: " + (t.getDepartment() != null ? t.getDepartment().getId() : "null") + " | Sec: " + (t.getSection() != null ? t.getSection().getId() : "null") + " | Year: " + t.getYear() + " | Captain: " + (t.getCaptain() != null ? t.getCaptain().getFullName() + " (Stg " + (t.getCaptain().getCurrentStage() > 0 ? t.getCaptain().getCurrentStage() : t.getCaptain().getStage()) + ")" : "null") + " | StageTeams: [" + stInfo + "] | Members count: " + (t.getMembers() != null ? t.getMembers().size() : 0));
        }

        System.out.println("================== ALL GROUP ACTIVITIES ====================");
        List<com.pragatix.entity.Activity> actList = activityRepository.findAll().stream()
                .filter(a -> (a.getModeType() != null && a.getModeType().contains("GROUP"))
                        || (a.getSubgroup() != null && a.getSubgroup().getCategory() != null && a.getSubgroup().getCategory().contains("GROUP")))
                .collect(Collectors.toList());
        for (var a : actList) {
            System.out.println("ACTIVITY ID: " + a.getId() + " | Name: " + a.getActivityName() + " | Mode: " + a.getModeType() + " | Stage: " + (a.getStage() != null ? a.getStage().getDisplayOrder() + " (" + a.getStage().getStageName() + ")" : "null") + " | Subgroup: " + (a.getSubgroup() != null ? a.getSubgroup().getName() + " (" + a.getSubgroup().getCategory() + ")" : "null"));
        }

        System.out.println("================== ALL ASSIGNMENTS ====================");
        List<com.pragatix.entity.ActivityAssignment> allAssigns = assignmentRepository.findAll();
        for (var a : allAssigns) {
            System.out.println("ASSIGN ID: " + a.getId() + " | Act: " + (a.getActivity() != null ? a.getActivity().getId() + " (" + a.getActivity().getActivityName() + " - " + a.getActivity().getModeType() + ")" : "null") + " | Stage: " + (a.getStage() != null ? a.getStage().getDisplayOrder() + " (" + a.getStage().getStageName() + ")" : "null") + " | Dept: " + (a.getDepartment() != null ? a.getDepartment().getId() : "null") + " | Sec: " + (a.getSection() != null ? a.getSection().getId() : "null") + " | Year: " + a.getYear() + " | Teacher: " + (a.getTeacher() != null ? a.getTeacher().getUsername() : "null"));
        }
        System.out.println("=========================================================================");
    }
}
