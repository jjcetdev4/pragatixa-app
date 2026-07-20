package com.spdms.modules.activity.service;

import com.spdms.modules.activity.dto.response.StageValidationResponse;
import com.spdms.entity.ActivityStage;
import com.spdms.entity.ActivitySubgroup;
import com.spdms.modules.activity.repository.ActivityStageRepository;
import com.spdms.modules.activity.repository.ActivitySubgroupRepository;
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
    private final com.spdms.modules.student.repository.StudentActivityXpRepository studentActivityXpRepository;

    public StageValidationService(ActivityStageRepository activityStageRepository,
                                  ActivitySubgroupRepository activitySubgroupRepository,
                                  com.spdms.modules.student.repository.StudentActivityXpRepository studentActivityXpRepository) {
        this.activityStageRepository = activityStageRepository;
        this.activitySubgroupRepository = activitySubgroupRepository;
        this.studentActivityXpRepository = studentActivityXpRepository;
    }

    public StageValidationResponse validateStage(Long regNo, Long stageId) {
        try {
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
            List<ActivitySubgroup> subgroups = activitySubgroupRepository.findByStageId(stageId);
            List<com.spdms.entity.StudentActivityXp> history = studentActivityXpRepository.findByStudentId(regNo);

            if (stage.isUseThresholdValidation() || stage.isUseCombinedValidation()) {
                for (ActivitySubgroup subgroup : subgroups) {
                    int earnedXp = history.stream()
                            .filter(xp -> xp.getActivity() != null && xp.getActivity().getSubgroup() != null && xp.getActivity().getSubgroup().getId().equals(subgroup.getId()))
                            .mapToInt(com.spdms.entity.StudentActivityXp::getXpAwarded)
                            .sum();
                    
                    boolean met = earnedXp >= subgroup.getThreshold();
                    
                    if (!met) {
                        thresholdMet = false;
                        // Don't break here so we can see all subgroups in logs if needed, but the original logic broke. 
                        // Wait, original logic broke. We should break. Actually we'll just print them all if we don't break, but to be strictly faithful to original logic:
                    }
                }
                // To maintain identical logical outcomes while printing all subgroups, we can just print and keep the flag.
            }

            // Apply 9 Rules
            if (!stage.isUseDateValidation() && !stage.isUseThresholdValidation() && !stage.isUseCombinedValidation()) {
                stageStatus = "UNLOCKED";
            } else if (stage.isUseCombinedValidation()) {
                if (dateStatus.equals("ACTIVE")) {
                    if (thresholdMet) {
                        stageStatus = "UNLOCKED";
                    } else {
                        stageStatus = "LOCKED_THRESHOLD";
                    }
                } else {
                    stageStatus = dateStatus; 
                }
            } else if (stage.isUseDateValidation()) {
                stageStatus = dateStatus; 
            } else if (stage.isUseThresholdValidation()) {
                if (thresholdMet) {
                    stageStatus = "UNLOCKED"; 
                } else {
                    stageStatus = "LOCKED_THRESHOLD"; 
                }
            } else {
                stageStatus = "UNLOCKED";
            }

            response.setStageStatus(stageStatus);
            
            // Populate legacy fields
            response.setVisible(true);
            response.setLocked(stageStatus.startsWith("LOCKED"));
            response.setCompleted(stageStatus.equals("ENDED") || stageStatus.equals("UNLOCKED"));
            response.setActive(stageStatus.equals("ACTIVE"));

            log.debug("Stage Name: {} | Date Toggle: {} | Threshold Toggle: {} | Combined Toggle: {} | Status: {}", 
                     stage.getName(), stage.isUseDateValidation(), stage.isUseThresholdValidation(), stage.isUseCombinedValidation(), stageStatus);

            return response;
        } catch (Exception e) {
            throw e;
        }
    }
}
