package com.spdms.modules.student.service;

import com.spdms.common.response.ApiResponse;
import com.spdms.entity.Activity;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.Student;
import com.spdms.modules.activity.dto.response.ActivityStageResponse;
import com.spdms.modules.activity.dto.response.ActivitySubgroupResponse;
import com.spdms.modules.activity.repository.ActivityRepository;
import com.spdms.modules.activity.service.ActivityStageService;
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
    private final StudentXpAggregator xpAggregator;
    private final StudentAssignmentResolver assignmentResolver;
    private final StudentStageAssembler stageAssembler;

    public StudentStageFacade(ActivityStageService activityStageService,
                              ActivityRepository activityRepository,
                              StudentXpAggregator xpAggregator,
                              StudentAssignmentResolver assignmentResolver,
                              StudentStageAssembler stageAssembler) {
        this.activityStageService = activityStageService;
        this.activityRepository = activityRepository;
        this.xpAggregator = xpAggregator;
        this.assignmentResolver = assignmentResolver;
        this.stageAssembler = stageAssembler;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<?> getStudentStages(Student student) {
        try {
            List<ActivityStageResponse> stages = activityStageService.getAllStages();

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
                            activitiesBySubgroup.computeIfAbsent(act.getSubgroup().getId(), k -> new ArrayList<>()).add(act);
                        }
                        allActivityIds.add(act.getId());
                    }
                }

                Map<Long, List<ActivityAssignment>> assignmentsByActivity = assignmentResolver.fetchAssignmentsByActivity(allActivityIds);
                StudentXpAggregator.AggregatedXp aggregatedXp = xpAggregator.aggregateXpForStudent(student.getId());

                stageAssembler.assembleStages(student, stages, activitiesBySubgroup, assignmentsByActivity, aggregatedXp);
            }
            return ResponseEntity.ok(ApiResponse.ok(stages));
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch student stages", e);
        }
    }
}
