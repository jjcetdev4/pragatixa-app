package com.spdms.modules.student.service;

import com.spdms.entity.Activity;
import com.spdms.entity.ActivityAssignment;
import com.spdms.entity.Student;
import com.spdms.modules.activity.dto.response.ActivityStageResponse;
import com.spdms.modules.activity.dto.response.ActivitySubgroupResponse;
import com.spdms.modules.activity.dto.response.StageValidationResponse;
import com.spdms.modules.activity.service.StageValidationService;
import com.spdms.modules.activity.dto.response.ActivityResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
            stage.setStageState(validation.getStageStatus());
            stage.setIsCurrentStage(stage.getDisplayOrder() != null && stage.getDisplayOrder().equals(student.getStage()));

            int studentMustXp = 0;
            int studentIndividualXp = 0;
            int studentGroupXp = 0;

            if ("COMPLETED".equals(validation.getStageStatus())) {
                studentMustXp = stage.getMustThreshold() != null ? stage.getMustThreshold() : 0;
                studentIndividualXp = stage.getIndividualThreshold() != null ? stage.getIndividualThreshold() : 0;
                studentGroupXp = stage.getGroupThreshold() != null ? stage.getGroupThreshold() : 0;
            } else if ("ACTIVE".equals(validation.getStageStatus())) {
                studentMustXp = student.getMustXp();
                studentIndividualXp = student.getIndividualXp();
                studentGroupXp = student.getGroupXp();
            } else {
                studentMustXp = 0;
                studentIndividualXp = 0;
                studentGroupXp = 0;
            }

            int overallTotalSubgroups = 0;
            int overallCompletedSubgroups = 0;
            double totalProgressPercentage = 0;

            List<ActivitySubgroupResponse> dynamicSubgroups = new ArrayList<>();

            if (stage.getSubgroups() != null) {
                for (ActivitySubgroupResponse subgroup : stage.getSubgroups()) {
                    Long subId = subgroup.getId();
                    List<Activity> activities = activitiesBySubgroup.getOrDefault(subId, java.util.Collections.emptyList());
                    List<ActivityResponse> enrichedActs = activityAssembler.enrichActivities(student, activities, assignmentsByActivity, aggregatedXp);

                    // Use the subgroup's name directly, with Title Case on the frontend
                    // Here we just map its properties
                    int thresh = subgroup.getThreshold() != null ? subgroup.getThreshold() : 0;
                    
                    // Since studentXP properties are hardcoded in the Student table for now (MustXp, IndividualXp, GroupXp),
                    // We must match subgroup name to determine which student XP to use.
                    // If subgroup name does not match, we'll calculate from completed activities.
                    int studentCategoryXp = 0;
                    String subName = subgroup.getName() != null ? subgroup.getName().toLowerCase() : "";
                    
                    if (subName.contains("must") || subName.contains("mandatory")) {
                        studentCategoryXp = studentMustXp;
                        stage.setStudentMustXp(studentCategoryXp);
                        stage.setMustThreshold(thresh);
                        stage.setMustCompleted(thresh > 0 && studentCategoryXp >= thresh);
                        stage.setMustRemaining(Math.max(0, thresh - studentCategoryXp));
                    } else if (subName.contains("individual")) {
                        studentCategoryXp = studentIndividualXp;
                        stage.setStudentIndividualXp(studentCategoryXp);
                        stage.setIndividualThreshold(thresh);
                        stage.setIndividualCompleted(thresh > 0 && studentCategoryXp >= thresh);
                        stage.setIndividualRemaining(Math.max(0, thresh - studentCategoryXp));
                    } else if (subName.contains("group") || subName.contains("team")) {
                        studentCategoryXp = studentGroupXp;
                        stage.setStudentGroupXp(studentCategoryXp);
                        stage.setGroupThreshold(thresh);
                        stage.setGroupCompleted(thresh > 0 && studentCategoryXp >= thresh);
                        stage.setGroupRemaining(Math.max(0, thresh - studentCategoryXp));
                    } else {
                        // For any future subgroup name, calculate xp directly from enriched activities
                        for (ActivityResponse act : enrichedActs) {
                            if ("COMPLETED".equals(act.getStatus()) && act.getAwardedXp() != null) {
                                studentCategoryXp += act.getAwardedXp();
                            }
                        }
                    }

                    boolean subCompleted = thresh > 0 && studentCategoryXp >= thresh;
                    
                    if (thresh > 0 || !enrichedActs.isEmpty()) {
                        overallTotalSubgroups++;
                        if (subCompleted) overallCompletedSubgroups++;
                        totalProgressPercentage += thresh > 0 ? Math.min(1.0, (double) studentCategoryXp / thresh) : 0;

                        ActivitySubgroupResponse resSub = new ActivitySubgroupResponse();
                        resSub.setId(subId);
                        resSub.setName(subgroup.getName()); // Do not modify, frontend will title case
                        resSub.setThreshold(thresh);
                        resSub.setActivities(enrichedActs);
                        dynamicSubgroups.add(resSub);
                    }
                }
            }

            stage.setSubgroups(dynamicSubgroups);
            stage.setOverallTotalSubgroups(overallTotalSubgroups);
            stage.setOverallCompletedSubgroups(overallCompletedSubgroups);

            double percentage = 0.0;
            if (overallTotalSubgroups > 0) {
                percentage = (totalProgressPercentage / overallTotalSubgroups) * 100.0;
            }
            stage.setOverallPercentage(percentage);
        }
    }
}
