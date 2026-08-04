package com.pragatix.modules.activity.service;

import com.pragatix.modules.activity.dto.response.StageValidationResponse;
import com.pragatix.entity.ActivityStage;
import com.pragatix.entity.ActivitySubgroup;
import com.pragatix.entity.Student;
import com.pragatix.modules.activity.repository.ActivityStageRepository;
import com.pragatix.modules.activity.repository.ActivitySubgroupRepository;
import com.pragatix.modules.activity.repository.ActivityRepository;
import com.pragatix.modules.student.repository.StudentRepository;
import com.pragatix.modules.student.repository.StudentActivityXpRepository;
import com.pragatix.entity.Activity;
import com.pragatix.entity.StudentActivityXp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StageValidationService {
    private static final Logger log = LoggerFactory.getLogger(StageValidationService.class);

    private final ActivityStageRepository activityStageRepository;
    private final StudentRepository studentRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final StudentActivityXpRepository studentActivityXpRepository;
    private final ActivityRepository activityRepository;
    private final com.pragatix.modules.activity.repository.ActivityStageMappingRepository activityStageMappingRepository;
    private final com.pragatix.repository.XpTransactionRepository xpTransactionRepository;
    private final com.pragatix.modules.student.service.StageXpSummaryService stageXpSummaryService;

    public StageValidationService(ActivityStageRepository activityStageRepository,
            StudentRepository studentRepository,
            ActivitySubgroupRepository activitySubgroupRepository,
            StudentActivityXpRepository studentActivityXpRepository,
            ActivityRepository activityRepository,
            com.pragatix.modules.activity.repository.ActivityStageMappingRepository activityStageMappingRepository,
            com.pragatix.repository.XpTransactionRepository xpTransactionRepository,
            com.pragatix.modules.student.service.StageXpSummaryService stageXpSummaryService) {
        this.activityStageRepository = activityStageRepository;
        this.studentRepository = studentRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
        this.activityRepository = activityRepository;
        this.activityStageMappingRepository = activityStageMappingRepository;
        this.xpTransactionRepository = xpTransactionRepository;
        this.stageXpSummaryService = stageXpSummaryService;
    }

    public StageValidationResponse validateStage(Student student, ActivityStage stage) {
        try {
            if (stage == null)
                throw new IllegalArgumentException("Stage not found");

            StageValidationResponse response = new StageValidationResponse();
            response.setUseDateValidation(stage.isUseDateValidation());
            response.setUseThresholdValidation(stage.isUseThresholdValidation());
            response.setUseCombinedValidation(stage.isUseCombinedValidation());

            LocalDateTime now = LocalDateTime.now();
            String stageStatus;

            int studentStage = student.getStage();
            int stageDisplayOrder = stage.getDisplayOrder();

            if (stageDisplayOrder < studentStage) {
                stageStatus = "COMPLETED";
                response.setStageStatus(stageStatus);
                response.setVisible(true);
                response.setLocked(true);
                response.setCompleted(true);
                response.setActive(false);
            } else if (stageDisplayOrder == studentStage) {
                stageStatus = "ACTIVE";
                response.setStageStatus(stageStatus);
                response.setVisible(true);
                response.setLocked(false);
                response.setCompleted(false);
                response.setActive(true);
            } else if (stageDisplayOrder == studentStage + 1) {
                stageStatus = "LOCKED";
                response.setStageStatus(stageStatus);
                response.setVisible(true);
                response.setLocked(true);
                response.setCompleted(false);
                response.setActive(false);
            } else {
                stageStatus = "LOCKED";
                response.setStageStatus(stageStatus);
                response.setVisible(true);
                response.setLocked(true);
                response.setCompleted(false);
                response.setActive(false);
            }

            return response;
        } catch (Exception e) {
            throw e;
        }
    }

    public StageValidationResponse validateStage(Long studentId, Long stageId) {
        ActivityStage stage = activityStageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found"));
        Student student = studentRepository.findById(studentId).orElse(null);
        return validateStage(student, stage);
    }

    public boolean isStageThresholdsMet(Student student, ActivityStage stage) {
        if (stage == null || student == null)
            return false;

        System.out.println("=====================================================");
        System.out.println("STAGE ENGINE - EVALUATING THRESHOLDS DYNAMICALLY");
        System.out.println("Which Stage object is loaded: " + stage.getName());
        System.out.println("Database Stage ID           : " + stage.getId());
        System.out.println("Total Threshold             : " + stage.getExpectedXp());
        System.out.println("Must Threshold              : " + stage.getMustThreshold());
        System.out.println("Individual Threshold        : " + stage.getIndividualThreshold());
        System.out.println("Group Threshold             : " + stage.getGroupThreshold());
        System.out.println("Repository method used      : activityStageRepository.findById/findByDisplayOrder");
        System.out.println("Student ID                  : " + student.getId());
        System.out.println("--- XP Values vs Thresholds ---");

        com.pragatix.modules.student.dto.StageXpSummary stageXp = stageXpSummaryService.getStageXp(student.getId(), stage.getDisplayOrder());

        int expectedXp = stage.getExpectedXp() != null ? stage.getExpectedXp() : 0;
        boolean expectedXpMet = stageXp.getTotalXp() >= expectedXp;
        System.out.println("Total XP      : " + stageXp.getTotalXp() + " >= " + expectedXp + " -> " + expectedXpMet);

        int mustThresh = stage.getMustThreshold() != null ? stage.getMustThreshold() : 0;
        int indThresh = stage.getIndividualThreshold() != null ? stage.getIndividualThreshold() : 0;
        int grpThresh = stage.getGroupThreshold() != null ? stage.getGroupThreshold() : 0;
        
        int evaluatedMustXp = stageXp.getMustXp();

        boolean mustMet = stageXp.getMustXp() >= mustThresh;
        boolean indMet = stageXp.getIndividualXp() >= indThresh;
        boolean grpMet = stageXp.getGroupXp() >= grpThresh;

        System.out.println("Must XP       : " + stageXp.getMustXp() + " >= " + mustThresh + " -> " + mustMet);
        System.out.println("Individual XP : " + stageXp.getIndividualXp() + " >= " + indThresh + " -> " + indMet);
        System.out.println("Group XP      : " + stageXp.getGroupXp() + " >= " + grpThresh + " -> " + grpMet);

        boolean allSubgroupsMet = mustMet && indMet && grpMet;

        boolean allMet = expectedXpMet && allSubgroupsMet;
        
        List<com.pragatix.entity.ActivityStageMapping> stageMappings = activityStageMappingRepository.findByStageId(stage.getId());
        List<Activity> allMappedActivities = new java.util.ArrayList<>();
        java.util.Set<Long> mappedIds = new java.util.HashSet<>();
        
        for (com.pragatix.entity.ActivityStageMapping mapping : stageMappings) {
            Activity act = mapping.getActivity();
            if (act != null) {
                allMappedActivities.add(act);
                mappedIds.add(act.getId());
            }
        }
        
        List<Activity> legacyActivities = activityRepository.findByStageId(stage.getId());
        for (Activity act : legacyActivities) {
            if (act != null && !mappedIds.contains(act.getId())) {
                allMappedActivities.add(act);
                mappedIds.add(act.getId());
            }
        }


        List<Activity> mandatoryMustActivities = allMappedActivities.stream()
                .filter(a -> {
                    if (a == null) return false;
                    boolean isActive = "ACTIVE".equalsIgnoreCase(a.getStatus()) || a.getStatus() == null;
                    boolean isReward = "Reward".equalsIgnoreCase(a.getXpType());
                    
                    // Determine subgroup from mapping first, fallback to activity global subgroup
                    com.pragatix.entity.ActivityStageMapping m = stageMappings.stream()
                            .filter(mp -> mp.getActivity() != null && mp.getActivity().getId().equals(a.getId()))
                            .findFirst().orElse(null);
                            
                    ActivitySubgroup effectiveSubgroup = null;
                    if (m != null && m.getSubgroup() != null) {
                        effectiveSubgroup = m.getSubgroup();
                    } else {
                        effectiveSubgroup = a.getSubgroup();
                    }
                    
                    boolean inMustSubgroup = false;
                    if (effectiveSubgroup != null) {
                        String cat = effectiveSubgroup.getCategory();
                        String name = effectiveSubgroup.getName();
                        if (cat != null && "must".equalsIgnoreCase(cat.trim())) {
                            inMustSubgroup = true;
                        } else if (name != null && name.toLowerCase().startsWith("must")) {
                            inMustSubgroup = true;
                        }
                    }
                    
                    boolean passed = isActive && isReward && inMustSubgroup;
                    System.out.println(String.format("Must Checklist Filter Result for ID %d '%s' -> passed: %b (isActive: %b, isReward: %b, inMustSubgroup: %b)", 
                        a.getId(), a.getName(), passed, isActive, isReward, inMustSubgroup));
                    return passed;
                })
                .collect(Collectors.toList());

        System.out.println("========== MANDATORY ACTIVITY FILTERING ==========");
        System.out.println("All Mapped Activities (raw size): " + allMappedActivities.size());
        for (Activity a : mandatoryMustActivities) {
            System.out.println(String.format("Mandatory Candidate - ID: %d, status: %s, xpType: %s, isMandatory: %b", 
                a.getId(), a.getStatus(), a.getXpType(), a.isMandatory()));
        }

        List<Long> requiredActivityIds = mandatoryMustActivities.stream()
                .map(Activity::getId)
                .collect(Collectors.toList());

        List<com.pragatix.entity.XpTransaction> allXpLogs = xpTransactionRepository.findByStudentRegNoAndStage(student.getRegNo(), stage.getDisplayOrder());
        
        List<Long> rawCompletedActivityIds = allXpLogs.stream()
                .filter(tx -> "APPROVED".equalsIgnoreCase(tx.getStatus()) && !tx.isPenalty() && tx.getActivity() != null)
                .map(tx -> tx.getActivity().getId())
                .collect(Collectors.toList());

        List<Long> uniqueCompletedActivityIds = rawCompletedActivityIds.stream()
                .distinct()
                .collect(Collectors.toList());
                
        int duplicateActivitiesIgnored = rawCompletedActivityIds.size() - uniqueCompletedActivityIds.size();

        List<Long> completedMustActivityIds = uniqueCompletedActivityIds.stream()
                .filter(requiredActivityIds::contains)
                .collect(Collectors.toList());

        List<Long> missingActivityIds = requiredActivityIds.stream()
                .filter(id -> !completedMustActivityIds.contains(id))
                .collect(Collectors.toList());

        boolean mustChecklistSatisfied = missingActivityIds.isEmpty();
        boolean mustXpSatisfied = evaluatedMustXp >= mustThresh;
        boolean mustSatisfied = mustXpSatisfied && mustChecklistSatisfied;

        System.out.println("========== MUST TEST =========");
        System.out.println("Student ID\n" + student.getId());
        System.out.println("Required Activities\n" + requiredActivityIds);
        System.out.println("Completed Activities\n" + rawCompletedActivityIds);
        System.out.println("Unique Completed Activities\n" + uniqueCompletedActivityIds);
        System.out.println("Missing Activities\n" + missingActivityIds);
        System.out.println("Duplicate Activities Ignored\n" + duplicateActivitiesIgnored);
        System.out.println("Must XP\n" + evaluatedMustXp);
        System.out.println("Must Threshold\n" + mustThresh);
        System.out.println("Checklist Passed\n" + mustChecklistSatisfied);
        System.out.println("Final Must Validation\n" + (mustSatisfied ? "PASSED" : "FAILED"));
        System.out.println("================================");

        if (!mustSatisfied) {
            allMet = false;
        }

        System.out.println(
                "Final Decision: " + (allMet ? "PROMOTED (All thresholds met)" : "PENDING (Thresholds not met)"));
        System.out.println("=====================================================");

        return allMet;
    }

    public boolean isStageThresholdsMet(Long studentId, Long stageId) {
        ActivityStage stage = activityStageRepository.findById(stageId).orElse(null);
        Student student = studentRepository.findById(studentId).orElse(null);
        return isStageThresholdsMet(student, stage);
    }
}
