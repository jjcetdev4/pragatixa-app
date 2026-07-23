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

            List<ActivityResponse> mustActivities = new ArrayList<>();
            List<ActivityResponse> individualActivities = new ArrayList<>();
            List<ActivityResponse> groupActivities = new ArrayList<>();

            if (stage.getSubgroups() != null) {
                for (ActivitySubgroupResponse subgroup : stage.getSubgroups()) {
                    Long subId = subgroup.getId();
                    List<Activity> activities = activitiesBySubgroup.getOrDefault(subId, java.util.Collections.emptyList());
                    
                    List<ActivityResponse> enrichedActs = activityAssembler.enrichActivities(
                            student, activities, assignmentsByActivity, aggregatedXp);

                    // Categorize Activities only
                    String subName = subgroup.getName() != null ? subgroup.getName().toLowerCase() : "";
                    String subCat = "";
                    String fullCat = subCat + " " + subName;

                    for (int i = 0; i < activities.size(); i++) {
                        Activity act = activities.get(i);
                        ActivityResponse res = enrichedActs.get(i);
                        
                        String actCat = act.getXpCategory() != null ? act.getXpCategory().toLowerCase() : "";
                        String finalCat = fullCat + " " + actCat;

                        if (finalCat.contains("must") || finalCat.contains("mandatory")) {
                            mustActivities.add(res);
                        } else if (finalCat.contains("individual")) {
                            individualActivities.add(res);
                        } else if (finalCat.contains("group") || finalCat.contains("team")) {
                            groupActivities.add(res);
                        }
                    }
                }
            }

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

            int overallTotalSubgroups = 0;
            int overallCompletedSubgroups = 0;

            // Reconstruct Subgroups into exactly 3 unified categories
            List<ActivitySubgroupResponse> unifiedCategories = new ArrayList<>();
            
            if (mustThresh > 0 || !mustActivities.isEmpty()) {
                overallTotalSubgroups++;
                if (mustCompleted) overallCompletedSubgroups++;
                
                ActivitySubgroupResponse mustSub = new ActivitySubgroupResponse();
                mustSub.setName("Must Activities");
                mustSub.setThreshold(mustThresh);
                mustSub.setActivities(mustActivities);
                unifiedCategories.add(mustSub);
            }
            
            if (indThresh > 0 || !individualActivities.isEmpty()) {
                overallTotalSubgroups++;
                if (indCompleted) overallCompletedSubgroups++;
                
                ActivitySubgroupResponse indSub = new ActivitySubgroupResponse();
                indSub.setName("Individual Activities");
                indSub.setThreshold(indThresh);
                indSub.setActivities(individualActivities);
                unifiedCategories.add(indSub);
            }
            
            if (grpThresh > 0 || !groupActivities.isEmpty()) {
                overallTotalSubgroups++;
                if (grpCompleted) overallCompletedSubgroups++;
                
                ActivitySubgroupResponse grpSub = new ActivitySubgroupResponse();
                grpSub.setName("Group Activities");
                grpSub.setThreshold(grpThresh);
                grpSub.setActivities(groupActivities);
                unifiedCategories.add(grpSub);
            }

            stage.setSubgroups(unifiedCategories);

            stage.setOverallTotalSubgroups(overallTotalSubgroups);
            stage.setOverallCompletedSubgroups(overallCompletedSubgroups);

            double percentage = 0.0;
            if (overallTotalSubgroups > 0) {
                double mustProg = mustThresh > 0 ? Math.min(1.0, (double) studentMustXp / mustThresh) : 0;
                double indProg = indThresh > 0 ? Math.min(1.0, (double) studentIndividualXp / indThresh) : 0;
                double grpProg = grpThresh > 0 ? Math.min(1.0, (double) studentGroupXp / grpThresh) : 0;
                percentage = ((mustProg + indProg + grpProg) / overallTotalSubgroups) * 100.0;
            }
            stage.setOverallPercentage(percentage);
        }
    }
}
