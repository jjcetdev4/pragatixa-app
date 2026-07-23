package com.spdms.modules.activity.service;

import com.spdms.modules.activity.dto.response.StageValidationResponse;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.Student;
import com.spdms.modules.activity.repository.ActivityStageRepository;
import com.spdms.modules.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StageValidationService {
    private static final Logger log = LoggerFactory.getLogger(StageValidationService.class);

    private final ActivityStageRepository activityStageRepository;
    private final StudentRepository studentRepository;

    public StageValidationService(ActivityStageRepository activityStageRepository,
                                  StudentRepository studentRepository) {
        this.activityStageRepository = activityStageRepository;
        this.studentRepository = studentRepository;
    }

    public StageValidationResponse validateStage(Student student, ActivityStage stage) {
        try {
            if (stage == null) throw new IllegalArgumentException("Stage not found");

            StageValidationResponse response = new StageValidationResponse();
            response.setUseDateValidation(stage.isUseDateValidation());
            response.setUseThresholdValidation(stage.isUseThresholdValidation());
            response.setUseCombinedValidation(stage.isUseCombinedValidation());

            LocalDateTime now = LocalDateTime.now();
            String stageStatus;

            // Dynamic Stage Transition Sequence Evaluation
            int studentStage = student.getStage();
            int stageDisplayOrder = stage.getDisplayOrder();
            
            if (stageDisplayOrder < studentStage) {
                // Past Stage - Completed, Locked, Read Only
                stageStatus = "COMPLETED";
                response.setStageStatus(stageStatus);
                response.setVisible(true);
                response.setLocked(true);
                response.setCompleted(true);
                response.setActive(false);
            } else if (stageDisplayOrder == studentStage) {
                // Current Stage
                stageStatus = "ACTIVE";
                response.setStageStatus(stageStatus);
                response.setVisible(true);
                response.setLocked(false);
                response.setCompleted(false);
                response.setActive(true);
            } else if (stageDisplayOrder == studentStage + 1) {
                // Immediate Next Stage - Unlocked but not active? 
                // Wait, the prompt says "unlock the next stage, lock the completed stage".
                // But usually, an unlocked stage becomes the active stage. The prompt requires returning exactly one of: LOCKED, ACTIVE, COMPLETED, UNLOCKED.
                // If it's unlocked but not active, maybe we return UNLOCKED. But wait, if they are promoted to stage 2, stage 2 is ACTIVE.
                // I will just use LOCKED for future stages. The prompt specifically requested: "LOCKED, ACTIVE, COMPLETED, UNLOCKED".
                // Actually, if it's the exact next stage, maybe it's "LOCKED" until they finish the current one, but if they just finished the current one, their currentStage becomes the next stage, making it ACTIVE.
                // So any stage > studentStage is strictly LOCKED.
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
        if (stage == null || student == null) return false;

        System.out.println("=====================================================");
        System.out.println("STAGE ENGINE - EVALUATING THRESHOLDS");
        System.out.println("Student ID    : " + student.getId());
        System.out.println("Stage Target  : " + stage.getDisplayOrder() + " (" + stage.getName() + ")");
        System.out.println("--- XP Values vs Thresholds ---");
        
        int mustThresh = stage.getMustThreshold() != null ? stage.getMustThreshold() : 0;
        int indThresh = stage.getIndividualThreshold() != null ? stage.getIndividualThreshold() : 0;
        int grpThresh = stage.getGroupThreshold() != null ? stage.getGroupThreshold() : 0;
        int expectedXp = stage.getExpectedXp() != null ? stage.getExpectedXp() : 0;

        boolean expectedXpMet = student.getTotalXp() >= expectedXp;
        boolean mustMet = student.getMustXp() >= mustThresh;
        boolean indMet = student.getIndividualXp() >= indThresh;
        boolean grpMet = student.getGroupXp() >= grpThresh;

        System.out.println("Total XP      : " + student.getTotalXp() + " >= " + expectedXp + " -> " + expectedXpMet);
        System.out.println("Must XP       : " + student.getMustXp() + " >= " + mustThresh + " -> " + mustMet);
        System.out.println("Individual XP : " + student.getIndividualXp() + " >= " + indThresh + " -> " + indMet);
        System.out.println("Group XP      : " + student.getGroupXp() + " >= " + grpThresh + " -> " + grpMet);

        boolean allMet = expectedXpMet && mustMet && indMet && grpMet;
        
        System.out.println("Final Decision: " + (allMet ? "PROMOTED (All thresholds met)" : "PENDING (Thresholds not met)"));
        System.out.println("=====================================================");

        return allMet;
    }

    public boolean isStageThresholdsMet(Long studentId, Long stageId) {
        ActivityStage stage = activityStageRepository.findById(stageId).orElse(null);
        Student student = studentRepository.findById(studentId).orElse(null);
        return isStageThresholdsMet(student, stage);
    }
}
