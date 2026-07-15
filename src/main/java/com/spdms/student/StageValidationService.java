package com.spdms.student;

import com.spdms.dto.StageValidationResponse;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.entity.DisciplineLog;
import com.spdms.repository.ActivityStageRepository;
import com.spdms.repository.ActivitySubgroupRepository;
import com.spdms.repository.DisciplineLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class StageValidationService {
    private static final Logger log = LoggerFactory.getLogger(StageValidationService.class);

    private final ActivityStageRepository activityStageRepository;
    private final ActivitySubgroupRepository activitySubgroupRepository;
    private final DisciplineLogRepository disciplineLogRepository;

    public StageValidationService(ActivityStageRepository activityStageRepository,
                                  ActivitySubgroupRepository activitySubgroupRepository,
                                  DisciplineLogRepository disciplineLogRepository) {
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.disciplineLogRepository = disciplineLogRepository;
    }

    public StageValidationResponse validateStage(Long studentId, Long stageId) {
        ActivityStage stage = activityStageRepository.findById(stageId)
                .orElseThrow(() -> new IllegalArgumentException("Stage not found"));

        StageValidationResponse response = new StageValidationResponse();
        response.setUseDateValidation(stage.isUseDateValidation());
        response.setUseThresholdValidation(stage.isUseThresholdValidation());
        response.setUseCombinedValidation(stage.isUseCombinedValidation());

        LocalDateTime now = LocalDateTime.now();
        String stageStatus;

        // Calculate Date Status
        String dateStatus = "ACTIVE";
        if (stage.getStartDateTime() != null && now.isBefore(stage.getStartDateTime())) {
            dateStatus = "LOCKED_BEFORE_START";
        } else if (stage.getEndDateTime() != null && now.isAfter(stage.getEndDateTime())) {
            dateStatus = "ENDED";
        }

        // Calculate Threshold Status
        boolean thresholdMet = true;
        if (stage.isUseThresholdValidation() || stage.isUseCombinedValidation()) {
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stageId);
            List<DisciplineLog> history = disciplineLogRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
            for (ActivitySubgroup subgroup : subgroups) {
                int earnedXp = history.stream()
                        .filter(logXp -> logXp.getSubgroup() != null && logXp.getSubgroup().getId().equals(subgroup.getId()))
                        .mapToInt(DisciplineLog::getPoints)
                        .sum();
                if (earnedXp < subgroup.getThreshold()) {
                    thresholdMet = false;
                    break;
                }
            }
        }

        // Apply 9 Rules
        if (!stage.isUseDateValidation() && !stage.isUseThresholdValidation() && !stage.isUseCombinedValidation()) {
            // Rule 4: No Validation
            stageStatus = "UNLOCKED";
        } else if (stage.isUseCombinedValidation()) {
            // Rule 3: Combined
            if (dateStatus.equals("ACTIVE")) {
                if (thresholdMet) {
                    stageStatus = "UNLOCKED"; // Case 6
                } else {
                    stageStatus = "LOCKED_THRESHOLD"; // Case 7
                }
            } else {
                stageStatus = dateStatus; // Case 8 (LOCKED_BEFORE_START or ENDED)
            }
        } else if (stage.isUseDateValidation()) {
            // Rule 1: Date Only
            stageStatus = dateStatus; // Cases 1, 2, 3 (LOCKED_BEFORE_START, ACTIVE, ENDED)
        } else if (stage.isUseThresholdValidation()) {
            // Rule 2: Threshold Only
            if (thresholdMet) {
                stageStatus = "UNLOCKED"; // Case 5
            } else {
                stageStatus = "LOCKED_THRESHOLD"; // Case 4
            }
        } else {
            stageStatus = "UNLOCKED";
        }

        response.setStageStatus(stageStatus);
        
        // Populate legacy fields just in case they are used elsewhere
        response.setVisible(true);
        response.setLocked(stageStatus.startsWith("LOCKED"));
        response.setCompleted(stageStatus.equals("ENDED") || stageStatus.equals("UNLOCKED"));
        response.setActive(stageStatus.equals("ACTIVE"));

        log.info("Stage Name: {} | Date Toggle: {} | Threshold Toggle: {} | Combined Toggle: {} | Status: {}", 
                 stage.getName(), stage.isUseDateValidation(), stage.isUseThresholdValidation(), stage.isUseCombinedValidation(), stageStatus);

        return response;
    }
}
