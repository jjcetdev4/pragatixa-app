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

            int studentMustXp = 0;
            int studentIndividualXp = 0;
            int studentGroupXp = 0;

            int overallCompletedSubgroups = 0;
            int overallTotalSubgroups = 0;

            if (stage.getSubgroups() != null) {
                for (ActivitySubgroupResponse subgroup : stage.getSubgroups()) {
                    Long subId = subgroup.getId();
                    List<Activity> activities = activitiesBySubgroup.getOrDefault(subId, java.util.Collections.emptyList());
                    
                    subgroup.setActivities(activityAssembler.enrichActivities(
                            student, activities, assignmentsByActivity, aggregatedXp));

                    // Calculate XP for this subgroup
                    int subgroupXp = 0;
                    for (Activity act : activities) {
                        subgroupXp += aggregatedXp.xpByActivityId.getOrDefault(act.getId(), 0);
                    }

                    String subName = subgroup.getName() != null ? subgroup.getName().toLowerCase() : "";
                    if (subName.contains("must")) {
                        studentMustXp += subgroupXp;
                        if (stage.getMustThreshold() == null || stage.getMustThreshold() == 0) {
                            stage.setMustThreshold(subgroup.getThreshold()); // Fallback if stage threshold is 0
                        }
                    } else if (subName.contains("individual")) {
                        studentIndividualXp += subgroupXp;
                        if (stage.getIndividualThreshold() == null || stage.getIndividualThreshold() == 0) {
                            stage.setIndividualThreshold(subgroup.getThreshold());
                        }
                    } else if (subName.contains("group")) {
                        studentGroupXp += subgroupXp;
                        if (stage.getGroupThreshold() == null || stage.getGroupThreshold() == 0) {
                            stage.setGroupThreshold(subgroup.getThreshold());
                        }
                    }
                }
            }

            // Fallback for null thresholds
            int mustThresh = stage.getMustThreshold() != null ? stage.getMustThreshold() : 0;
            int indThresh = stage.getIndividualThreshold() != null ? stage.getIndividualThreshold() : 0;
            int grpThresh = stage.getGroupThreshold() != null ? stage.getGroupThreshold() : 0;

            stage.setMustThreshold(mustThresh);
            stage.setIndividualThreshold(indThresh);
            stage.setGroupThreshold(grpThresh);

            stage.setStudentMustXp(studentMustXp);
            stage.setStudentIndividualXp(studentIndividualXp);
            stage.setStudentGroupXp(studentGroupXp);

            boolean mustCompleted = mustThresh > 0 && studentMustXp >= mustThresh;
            boolean indCompleted = indThresh > 0 && studentIndividualXp >= indThresh;
            boolean grpCompleted = grpThresh > 0 && studentGroupXp >= grpThresh;

            stage.setMustCompleted(mustCompleted);
            stage.setIndividualCompleted(indCompleted);
            stage.setGroupCompleted(grpCompleted);

            stage.setMustRemaining(Math.max(0, mustThresh - studentMustXp));
            stage.setIndividualRemaining(Math.max(0, indThresh - studentIndividualXp));
            stage.setGroupRemaining(Math.max(0, grpThresh - studentGroupXp));

            if (mustThresh > 0) overallTotalSubgroups++;
            if (indThresh > 0) overallTotalSubgroups++;
            if (grpThresh > 0) overallTotalSubgroups++;

            if (mustCompleted) overallCompletedSubgroups++;
            if (indCompleted) overallCompletedSubgroups++;
            if (grpCompleted) overallCompletedSubgroups++;

            stage.setOverallTotalSubgroups(overallTotalSubgroups);
            stage.setOverallCompletedSubgroups(overallCompletedSubgroups);

            double percentage = 0.0;
            if (overallTotalSubgroups > 0) {
                // Calculate percentage based on XP progress towards thresholds
                double mustProg = mustThresh > 0 ? Math.min(1.0, (double) studentMustXp / mustThresh) : 0;
                double indProg = indThresh > 0 ? Math.min(1.0, (double) studentIndividualXp / indThresh) : 0;
                double grpProg = grpThresh > 0 ? Math.min(1.0, (double) studentGroupXp / grpThresh) : 0;
                percentage = ((mustProg + indProg + grpProg) / overallTotalSubgroups) * 100.0;
            }
            stage.setOverallPercentage(percentage);
        }
    }
}
