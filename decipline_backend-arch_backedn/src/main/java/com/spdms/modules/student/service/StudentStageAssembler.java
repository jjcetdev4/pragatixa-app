package com.spdms.modules.student.service;

import com.spdms.entity.Activity;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.Student;
import com.spdms.modules.activity.dto.response.ActivityStageResponse;
import com.spdms.modules.activity.dto.response.ActivitySubgroupResponse;
import com.spdms.modules.activity.dto.response.StageValidationResponse;
import com.spdms.modules.activity.service.StageValidationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class StudentStageAssembler {

    private final StageValidationService stageValidationService;
    private final StudentActivityAssembler activityAssembler;

    public StudentStageAssembler(StageValidationService stageValidationService,
                                 StudentActivityAssembler activityAssembler) {
        this.stageValidationService = stageValidationService;
        this.activityAssembler = activityAssembler;
    }

    public void assembleStages(Student student,
                               List<ActivityStageResponse> stages,
                               Map<Long, List<Activity>> activitiesBySubgroup,
                               Map<Long, List<ActivityAssignment>> assignmentsByActivity,
                               StudentXpAggregator.AggregatedXp aggregatedXp) {

        for (ActivityStageResponse stage : stages) {
            StageValidationResponse validation = stageValidationService.validateStage(student.getId(), stage.getId());
            stage.setValidation(validation);
            stage.setVisible(validation.isVisible());
            stage.setLocked(validation.isLocked());
            stage.setIsCompleted(validation.isCompleted());
            stage.setIsActive(validation.isActive());
            stage.setStageStatus(validation.getStageStatus());

            if (stage.getSubgroups() != null) {
                for (ActivitySubgroupResponse subgroup : stage.getSubgroups()) {
                    Long subId = subgroup.getId();
                    List<Activity> activities = activitiesBySubgroup.getOrDefault(subId, java.util.Collections.emptyList());
                    
                    subgroup.setActivities(activityAssembler.enrichActivities(
                            student, activities, assignmentsByActivity, aggregatedXp));
                }
            }
        }
    }
}
