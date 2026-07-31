package com.pragatix.modules.student.service;

import com.pragatix.common.response.ApiResponse;
import com.pragatix.entity.Activity;
import com.pragatix.entity.ActivityAssignment;
import com.pragatix.entity.Student;
import com.pragatix.modules.activity.dto.response.ActivityStageResponse;
import com.pragatix.modules.activity.dto.response.ActivitySubgroupResponse;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.activity.service.ActivityStageService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudentStageFacade {

    private final ActivityStageService activityStageService;
    private final ActivityRepository activityRepository;
    private final StudentAssignmentResolver assignmentResolver;
    private final StudentStageAssembler stageAssembler;

    public StudentStageFacade(ActivityStageService activityStageService,
            ActivityRepository activityRepository,
            StudentAssignmentResolver assignmentResolver,
            StudentStageAssembler stageAssembler) {
        this.activityStageService = activityStageService;
        this.activityRepository = activityRepository;
        this.assignmentResolver = assignmentResolver;
        this.stageAssembler = stageAssembler;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getStudentStages(Student student) {
        try {
            com.pragatix.enums.AcademicYear acYear = com.pragatix.enums.AcademicYear.fromStudent(student);
            List<ActivityStageResponse> stages = activityStageService.getAllStages(acYear);

            if (stages != null && !stages.isEmpty()) {
                List<Long> subgroupIds = new ArrayList<>();
                for (ActivityStageResponse stage : stages) {
                    if (stage.getSubgroups() != null) {
                        for (ActivitySubgroupResponse subgroup : stage.getSubgroups()) {
                            subgroupIds.add(subgroup.getId());
                        }
                    }
                }

                Map<Long, List<Activity>> activitiesBySubgroup = new HashMap<>();
                List<Long> allActivityIds = new ArrayList<>();
                if (!subgroupIds.isEmpty()) {
                    List<Activity> allActivities = activityRepository.findBySubgroupIdIn(subgroupIds);
                    for (Activity act : allActivities) {
                        if (act.getSubgroup() != null) {
                            activitiesBySubgroup.computeIfAbsent(act.getSubgroup().getId(), k -> new ArrayList<>())
                                    .add(act);
                        }
                        allActivityIds.add(act.getId());
                    }
                }

                Map<Long, List<ActivityAssignment>> assignmentsByActivity = assignmentResolver
                        .fetchAssignmentsByActivity(allActivityIds);
                stageAssembler.assembleStages(student, stages, activitiesBySubgroup, assignmentsByActivity);
            }
            return ResponseEntity.ok(ApiResponse.ok(stages));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch student stages", e);
        }
    }
}
